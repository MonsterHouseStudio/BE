package com.monsterhouse.notification;

import com.monsterhouse.notification.entity.NotificationOutbox;
import com.monsterhouse.notification.entity.OutboxStatus;
import com.monsterhouse.notification.repository.NotificationOutboxRepository;
import com.monsterhouse.notification.sender.NotificationChannel;
import com.monsterhouse.notification.sender.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 실패한 알림을 주기적으로 재발송합니다.
 *
 * ★ @SchedulerLock 이 반드시 필요합니다.
 *   파드가 여러 대면 같은 아웃박스 행을 동시에 집어 중복 발송합니다.
 *   고객이 확정 메일을 두 번 받는 건 단순 실수로 보이지 않습니다.
 *
 * ★ lockAtMostFor 와 배치 크기의 관계
 *   한 번에 BATCH_SIZE(50) 건만 처리합니다. 장애가 길어져 수천 건이 쌓였을 때
 *   한 배치가 전부 보내려 들면 lockAtMostFor(5분)를 넘겨 락이 풀리고,
 *   그 순간 다른 파드가 같은 건을 또 보냅니다. 나눠서 여러 번 도는 편이 안전합니다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.notification.retry.enabled", havingValue = "true",
        matchIfMissing = true)
public class OutboxDispatcher {

    private static final int BATCH_SIZE = 50;

    private final NotificationOutboxRepository outboxRepository;
    private final OutboxRecorder outboxRecorder;
    private final Map<NotificationChannel, NotificationSender> senders =
            new EnumMap<>(NotificationChannel.class);

    public OutboxDispatcher(NotificationOutboxRepository outboxRepository,
                            OutboxRecorder outboxRecorder,
                            List<NotificationSender> senderList) {
        this.outboxRepository = outboxRepository;
        this.outboxRecorder = outboxRecorder;
        senderList.forEach(s -> senders.put(s.channel(), s));
    }

    @Scheduled(fixedDelayString = "${app.notification.retry.interval-ms:60000}")
    @SchedulerLock(name = "notificationOutboxDispatch",
            lockAtLeastFor = "PT10S", lockAtMostFor = "PT5M")
    public void dispatch() {
        List<NotificationOutbox> due = outboxRepository.findDue(
                LocalDateTime.now(), PageRequest.of(0, BATCH_SIZE));

        if (due.isEmpty()) {
            return;
        }
        log.info("알림 재발송 시작. 대상={}건", due.size());

        int sent = 0;
        for (NotificationOutbox outbox : due) {
            NotificationSender sender = senders.get(outbox.getChannel());

            // 채널이 꺼졌다면 계속 붙들고 있을 이유가 없습니다.
            if (sender == null || !sender.isEnabled()) {
                outboxRecorder.markFailed(outbox.getId(), "채널 비활성 또는 미등록");
                continue;
            }

            try {
                sender.send(outbox.toMessage());
                outboxRecorder.markSent(outbox.getId());
                sent++;
            } catch (Exception e) {
                outboxRecorder.markFailed(outbox.getId(),
                        e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        long givenUp = outboxRepository.countByStatus(OutboxStatus.GIVEN_UP);
        log.info("알림 재발송 완료. 성공={}건 / 포기 누적={}건", sent, givenUp);

        // 포기한 건은 사람이 봐야 합니다. 로그로만 두면 아무도 안 봅니다.
        if (givenUp > 0) {
            log.warn("재시도를 포기한 알림이 {}건 있습니다. 확인이 필요합니다.", givenUp);
        }
    }
}