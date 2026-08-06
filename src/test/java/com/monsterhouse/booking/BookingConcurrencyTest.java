package com.monsterhouse.booking;

import com.monsterhouse.booking.dto.request.BookingCreateRequest;
import com.monsterhouse.booking.entity.*;
import com.monsterhouse.booking.repository.*;
import com.monsterhouse.booking.service.BookingService;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ★★★ 기획서 §4.3 — "이 테스트 코드 자체가 포트폴리오 자산이다."
 *
 * 검증하는 것:
 *  1. 같은 슬롯에 10명이 동시에 몰려도 정확히 1건만 성공하는가
 *  2. 상품이 달라도 시간이 겹치면 막히는가 (sharedResource=true)
 *  3. 취소된 슬롯을 다시 예약할 수 있는가 (slot_key NULL 처리 검증)
 *  4. 겹치지 않는 슬롯은 동시 요청이어도 전부 성공하는가 (과잉 직렬화 아님을 확인)
 */
@DisplayName("예약 동시성 제어")
class BookingConcurrencyTest extends IntegrationTestSupport {

    private static final int THREAD_COUNT = 10;

    @Autowired
    private BookingService bookingService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private AvailabilityRepository availabilityRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private BookingDayLockRepository dayLockRepository;

    private Product bodyProfile;
    private Product motivation;
    private LocalDate targetDate;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAllInBatch();
        dayLockRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        availabilityRepository.deleteAllInBatch();

        bodyProfile = productRepository.save(Product.builder()
                .type(ProductType.PHOTO)
                .nameKo("보정본 3장")
                .nameJa("レタッチ3枚")
                .durationMin(90)
                .price(BigDecimal.valueOf(250_000))
                .currency("KRW")
                .active(true)
                .bookable(true)
                .sortOrder(1)
                .build());

        motivation = productRepository.save(Product.builder()
                .type(ProductType.VIDEO)
                .nameKo("모티베이션")
                .nameJa("モチベーション")
                .durationMin(60)
                .price(BigDecimal.valueOf(250_000))
                .currency("KRW")
                .active(true)
                .bookable(true)
                .sortOrder(2)
                .build());

        // 7일 뒤로 잡아 리드타임/기간 제약을 확실히 통과시킵니다.
        targetDate = LocalDate.now().plusDays(7);

        availabilityRepository.save(Availability.builder()
                .dayOfWeek(targetDate.getDayOfWeek())
                .openTime(LocalTime.of(10, 0))
                .closeTime(LocalTime.of(20, 0))
                .active(true)
                .build());
    }

    @Test
    @DisplayName("10명이 같은 슬롯을 동시에 예약하면 1건만 성공한다")
    void onlyOneSucceedsOnSameSlot() throws InterruptedException {
        LocalDateTime slot = targetDate.atTime(14, 0);

        ConcurrencyResult result = runConcurrently(THREAD_COUNT,
                i -> bookingService.create(request(bodyProfile.getId(), slot, i), LocaleCode.KO));

        assertThat(result.success()).isEqualTo(1);
        assertThat(result.failure()).isEqualTo(THREAD_COUNT - 1);

        List<Booking> saved = bookingRepository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getStartAt()).isEqualTo(slot);
        assertThat(saved.get(0).getStatus()).isEqualTo(BookingStatus.REQUESTED);
    }

    @Test
    @DisplayName("상품이 달라도 시간이 겹치면 1건만 성공한다 (촬영팀 공유 자원)")
    void overlappingDifferentProductsAreBlocked() throws InterruptedException {
        // 바디프로필 14:00~15:30 vs 모티베이션 14:30~15:30 → 겹침
        LocalDateTime slotA = targetDate.atTime(14, 0);
        LocalDateTime slotB = targetDate.atTime(14, 30);

        ConcurrencyResult result = runConcurrently(THREAD_COUNT, i -> {
            boolean even = i % 2 == 0;
            bookingService.create(
                    request(even ? bodyProfile.getId() : motivation.getId(),
                            even ? slotA : slotB, i),
                    LocaleCode.KO);
        });

        assertThat(result.success()).isEqualTo(1);
        assertThat(bookingRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("겹치지 않는 슬롯은 동시 요청이어도 모두 성공한다")
    void nonOverlappingSlotsAllSucceed() throws InterruptedException {
        // 90분 + 버퍼 0분 → 10:00, 11:30, 13:00 ... 은 서로 겹치지 않음
        ConcurrencyResult result = runConcurrently(5, i -> {
            LocalDateTime slot = targetDate.atTime(10, 0).plusMinutes(90L * i);
            bookingService.create(request(bodyProfile.getId(), slot, i), LocaleCode.KO);
        });

        assertThat(result.success()).isEqualTo(5);
        assertThat(bookingRepository.findAll()).hasSize(5);
    }

    @Test
    @DisplayName("취소된 슬롯은 다시 예약할 수 있다 (slot_key NULL 처리)")
    void canRebookAfterCancel() {
        LocalDateTime slot = targetDate.atTime(16, 0);

        var first = bookingService.create(request(bodyProfile.getId(), slot, 1), LocaleCode.KO);
        bookingService.cancelByAdmin(
                bookingRepository.findByBookingCode(first.bookingCode()).orElseThrow().getId(),
                "테스트 취소");

        // 기획서 원안의 UNIQUE(product_id, start_at) 였다면 여기서 실패했을 것입니다.
        var second = bookingService.create(request(bodyProfile.getId(), slot, 2), LocaleCode.KO);

        assertThat(second.bookingCode()).isNotEqualTo(first.bookingCode());
        assertThat(bookingRepository.findAll())
                .filteredOn(b -> b.getStatus() != BookingStatus.CANCELED)
                .hasSize(1);
    }

    // ===================== 헬퍼 =====================

    private BookingCreateRequest request(Long productId, LocalDateTime startAt, int index) {
        return new BookingCreateRequest(
                productId,
                startAt,
                "테스터" + index,
                "010-0000-000" + (index % 10),
                "tester" + index + "@example.com",
                "동시성 테스트 " + index,
                true,
                java.util.List.of(),   // 옵션 없음
                null
        );
    }

    /**
     * CountDownLatch 두 개가 핵심입니다.
     *   ready  — 모든 스레드가 출발선에 설 때까지 대기 (동시성을 실제로 만듦)
     *   done   — 전부 끝날 때까지 메인 스레드가 대기
     * latch 없이 for 문으로 submit 만 하면 순차 실행되어 아무것도 검증하지 못합니다.
     */
    private ConcurrencyResult runConcurrently(int threadCount, ThrowingTask task)
            throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();
        // 예외를 그냥 삼키면 "몇 건 실패"만 알 뿐 왜 실패했는지 알 수 없습니다.
        // 슬롯 충돌로 막힌 것과 락 타임아웃·버그로 죽은 것을 구분하려면 사유가 필요합니다.
        Queue<String> errors = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    ready.await();
                    task.run(index);
                    success.incrementAndGet();
                } catch (Exception e) {
                    failure.incrementAndGet();
                    Throwable root = e;
                    while (root.getCause() != null && root.getCause() != root) {
                        root = root.getCause();
                    }
                    errors.add(e.getClass().getSimpleName() + " / "
                            + root.getClass().getSimpleName() + ": " + root.getMessage());
                } finally {
                    done.countDown();
                }
            });
        }

        ready.countDown();                       // 출발 신호
        done.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        errors.forEach(msg -> System.out.println("[concurrency-failure] " + msg));

        return new ConcurrencyResult(success.get(), failure.get(), List.copyOf(errors));
    }

    @FunctionalInterface
    private interface ThrowingTask {
        void run(int index) throws Exception;
    }

    private record ConcurrencyResult(int success, int failure, List<String> errors) {
    }
}