package com.monsterhouse.notification;

import com.monsterhouse.notification.entity.NotificationOutbox;
import com.monsterhouse.notification.entity.OutboxStatus;
import com.monsterhouse.notification.repository.NotificationOutboxRepository;
import com.monsterhouse.notification.sender.NotificationChannel;
import com.monsterhouse.notification.sender.NotificationMessage;
import com.monsterhouse.notification.sender.NotificationSender;
import com.monsterhouse.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 알림 재시도(아웃박스) 검증.
 *
 * ★ 왜 실제 DB 로 하는가
 *   재시도의 핵심은 "발송이 실패한 뒤에도 흔적이 남아 있는가"입니다.
 *   이건 트랜잭션 경계 문제라 목으로는 검증되지 않습니다.
 *   특히 OutboxRecorder 가 REQUIRES_NEW 로 즉시 커밋하는지를 봐야 합니다.
 *
 * ★ 왜 @MockBean 을 쓰지 않는가
 *   처음엔 MailNotificationSender 를 @MockBean 으로 바꿨는데 컨텍스트가 아예 뜨지 않았습니다.
 *   NotificationService 생성자가 senderList 를 EnumMap 에 담는데,
 *   목의 channel() 은 컨텍스트 생성 시점에 null 을 돌려줍니다(스텁은 @BeforeEach 에서야 설정됨).
 *   EnumMap.put(null, ...) 은 NPE 입니다.
 *
 *   그래서 스텁 sender 를 직접 만들어 NotificationService/OutboxDispatcher 를 손으로 조립합니다.
 *   OutboxRecorder 는 스프링 빈을 그대로 써서 REQUIRES_NEW 가 실제로 동작하게 둡니다.
 *   부수 효과로 공유 컨텍스트를 그대로 재사용해 테스트가 빨라집니다.
 *
 * ⚠ 이 방식이라 dispatch() 에는 @SchedulerLock 프록시가 적용되지 않습니다.
 *   ShedLock 자체는 파드 2대를 띄운 실험으로 따로 검증했습니다(중복 실행 0).
 *   여기서는 재시도 상태 기계와 영속성만 봅니다.
 */
@DisplayName("알림 재시도(아웃박스)")
class NotificationOutboxTest extends IntegrationTestSupport {

    @Autowired private OutboxRecorder outboxRecorder;
    @Autowired private NotificationOutboxRepository outboxRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private StubSender stub;
    private NotificationService notificationService;
    private OutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAllInBatch();

        stub = new StubSender();
        notificationService = new NotificationService(List.of(stub), outboxRecorder);
        dispatcher = new OutboxDispatcher(outboxRepository, outboxRecorder, List.of(stub));
    }

    @Test
    @DisplayName("발송이 실패해도 유실되지 않고 재시도 대상으로 남는다")
    void failedSendStaysPending() {
        stub.failWith(new RuntimeException("SMTP 연결 실패"));

        notificationService.send(NotificationChannel.MAIL,
                NotificationMessage.of("owner@example.com", "예약 신청", "본문"));

        List<NotificationOutbox> all = outboxRepository.findAll();
        assertThat(all).hasSize(1);

        NotificationOutbox row = all.get(0);
        assertThat(row.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(row.getAttempts()).isEqualTo(1);
        assertThat(row.getLastError()).contains("SMTP 연결 실패");
        // 즉시 재시도하면 죽어 있는 서버를 계속 때립니다. 1분 뒤로 밀려야 합니다.
        assertThat(row.getNextAttemptAt()).isAfter(LocalDateTime.now().plusSeconds(30));
    }

    @Test
    @DisplayName("성공하면 SENT 로 기록되고 재시도 대상에서 빠진다")
    void successMarksSent() {
        notificationService.send(NotificationChannel.MAIL,
                NotificationMessage.of("owner@example.com", "예약 확정", "본문"));

        NotificationOutbox row = outboxRepository.findAll().get(0);
        assertThat(row.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(row.getSentAt()).isNotNull();
        assertThat(due()).isEmpty();
    }

    @Test
    @DisplayName("★ 실패한 알림을 배치가 재발송해 복구한다")
    void dispatcherRetriesAndRecovers() {
        stub.failWith(new RuntimeException("SMTP 일시 장애"));

        notificationService.send(NotificationChannel.MAIL,
                NotificationMessage.of("owner@example.com", "예약 신청", "본문"));

        NotificationOutbox pending = outboxRepository.findAll().get(0);
        assertThat(pending.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(stub.attempts()).isEqualTo(1);

        // 백오프 때문에 아직 재시도 대상이 아닙니다. 1분을 기다릴 수 없으니 시각을 당깁니다.
        makeDue(pending.getId());

        // 외부 API 가 살아났다고 가정
        stub.recover();
        dispatcher.dispatch();

        NotificationOutbox recovered = outboxRepository.findById(pending.getId()).orElseThrow();
        assertThat(recovered.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(recovered.getSentAt()).isNotNull();
        assertThat(stub.attempts()).isEqualTo(2);   // 최초 1회 + 재시도 1회
    }

    @Test
    @DisplayName("5회 실패하면 포기하고 GIVEN_UP 으로 남긴다")
    void givesUpAfterMaxAttempts() {
        stub.failWith(new RuntimeException("계속 실패"));

        notificationService.send(NotificationChannel.MAIL,
                NotificationMessage.of("owner@example.com", "예약 신청", "본문"));

        Long id = outboxRepository.findAll().get(0).getId();

        // 남은 4회를 배치로 소진시킵니다. 매번 백오프를 무시하도록 시각을 당깁니다.
        for (int i = 0; i < NotificationOutbox.MAX_ATTEMPTS - 1; i++) {
            makeDue(id);
            dispatcher.dispatch();
        }

        NotificationOutbox row = outboxRepository.findById(id).orElseThrow();
        assertThat(row.getAttempts()).isEqualTo(NotificationOutbox.MAX_ATTEMPTS);
        assertThat(row.getStatus()).isEqualTo(OutboxStatus.GIVEN_UP);

        // 포기한 건은 더 이상 자동 재시도하지 않습니다. 사람이 봐야 합니다.
        assertThat(outboxRepository.findDue(
                LocalDateTime.now().plusDays(365), PageRequest.of(0, 10))).isEmpty();
        assertThat(outboxRepository.countByStatus(OutboxStatus.GIVEN_UP)).isEqualTo(1);
    }

    @Test
    @DisplayName("백오프가 지수적으로 늘어난다 (1 → 2 → 4분)")
    void backoffGrowsExponentially() {
        stub.failWith(new RuntimeException("실패"));

        notificationService.send(NotificationChannel.MAIL,
                NotificationMessage.of("owner@example.com", "제목", "본문"));
        Long id = outboxRepository.findAll().get(0).getId();

        // attempts=1 → 1분 뒤
        assertThat(minutesUntilNextAttempt(id)).isBetween(0L, 1L);

        makeDue(id);
        dispatcher.dispatch();      // attempts=2 → 2분 뒤
        assertThat(minutesUntilNextAttempt(id)).isBetween(1L, 2L);

        makeDue(id);
        dispatcher.dispatch();      // attempts=3 → 4분 뒤
        assertThat(minutesUntilNextAttempt(id)).isBetween(3L, 4L);
    }

    @Test
    @DisplayName("채널이 꺼져 있으면 아웃박스에 쌓지 않는다")
    void disabledChannelIsNotRecorded() {
        // 쌓아두면 나중에 채널을 켰을 때 몇 달 전 알림이 한꺼번에 쏟아집니다.
        stub.disable();

        notificationService.send(NotificationChannel.MAIL,
                NotificationMessage.of("owner@example.com", "예약 신청", "본문"));

        assertThat(outboxRepository.findAll()).isEmpty();
        assertThat(stub.attempts()).isZero();
    }

    // ===================== 헬퍼 =====================

    private List<NotificationOutbox> due() {
        return outboxRepository.findDue(LocalDateTime.now().plusDays(1), PageRequest.of(0, 10));
    }

    /** 백오프를 기다리지 않고 즉시 재시도 대상으로 만듭니다. */
    private void makeDue(Long id) {
        jdbcTemplate.update(
                "UPDATE notification_outbox SET next_attempt_at = ? WHERE id = ?",
                LocalDateTime.now().minusSeconds(1), id);
    }

    private long minutesUntilNextAttempt(Long id) {
        LocalDateTime next = outboxRepository.findById(id).orElseThrow().getNextAttemptAt();
        return java.time.Duration.between(LocalDateTime.now(), next).toMinutes();
    }

    /**
     * 실패/성공을 마음대로 조종하는 sender.
     * Mockito 목과 달리 channel() 이 생성 즉시 올바른 값을 돌려주므로
     * EnumMap 조립 시점에 NPE 가 나지 않습니다.
     */
    private static final class StubSender implements NotificationSender {
        private final AtomicInteger attempts = new AtomicInteger();
        private RuntimeException failure;
        private boolean enabled = true;

        void failWith(RuntimeException e) { this.failure = e; }
        void recover() { this.failure = null; }
        void disable() { this.enabled = false; }
        int attempts() { return attempts.get(); }

        @Override public NotificationChannel channel() { return NotificationChannel.MAIL; }
        @Override public boolean isEnabled() { return enabled; }

        @Override public void send(NotificationMessage message) {
            attempts.incrementAndGet();
            if (failure != null) throw failure;
        }
    }
}
