package com.monsterhouse.booking;

import com.monsterhouse.booking.dto.request.BookingCreateRequest;
import com.monsterhouse.booking.entity.Availability;
import com.monsterhouse.booking.entity.Booking;
import com.monsterhouse.booking.entity.BookingStatus;
import com.monsterhouse.booking.entity.Product;
import com.monsterhouse.booking.entity.ProductType;
import com.monsterhouse.booking.repository.AvailabilityRepository;
import com.monsterhouse.booking.repository.BookingDayLockRepository;
import com.monsterhouse.booking.repository.BookingRepository;
import com.monsterhouse.booking.repository.ProductRepository;
import com.monsterhouse.booking.service.BookingService;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 예약 동시성 부하 실험.
 *
 * ★ 이 클래스는 "검증"이 아니라 "측정"이 목적입니다.
 *   BookingConcurrencyTest 가 설계가 맞는지 확인한다면,
 *   여기서는 규모를 키웠을 때도 그 성질이 유지되는지를 봅니다.
 *
 * 조건을 기존 테스트와 맞춘 지점:
 *   · 실제 MySQL 8 (Testcontainers) — H2 로는 갭 락과 REPEATABLE READ 스냅샷이
 *     재현되지 않아 실험 자체가 무의미해집니다.
 *   · BookingService 직접 호출 (컨트롤러 우회) — 기존 테스트와 동일합니다.
 *     HTTP 계층을 끼우면 직렬화·네트워크 비용이 섞여 락 동작을 못 봅니다.
 *   · CountDownLatch 2개로 동시 출발 (ready/done).
 *
 * 결과는 RESULT| 로 시작하는 줄로 출력해 기계적으로 수집합니다.
 */
@DisplayName("예약 동시성 부하 실험")
class BookingConcurrencyLoadTest extends IntegrationTestSupport {

    private static final int REPEAT = 3;

    @Autowired private BookingService bookingService;
    @Autowired private ProductRepository productRepository;
    @Autowired private AvailabilityRepository availabilityRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingDayLockRepository dayLockRepository;

    @Test
    @DisplayName("실험 1~3 실행")
    void runLoadExperiments() throws Exception {
        System.out.println("RESULT|scenario|n|rep|success|failure|deadlock|dbRows|elapsedMs");

        experiment1();
        experiment2();
        experiment3();
    }

    // =====================================================================
    //  실험 1 — 동일 슬롯 경쟁 스케일
    //  같은 날짜·같은 시각에 N 건이 동시에 몰립니다. 가장 강한 경쟁 조건입니다.
    //  기대: 성공 1 / 데드락 0 / DB 1행
    // =====================================================================
    private void experiment1() throws Exception {
        for (int n : new int[]{5, 20, 50}) {
            for (int rep = 1; rep <= REPEAT; rep++) {
                Fixture f = resetAndSeed();
                LocalDateTime slot = f.date.atTime(14, 0);

                Run run = runConcurrently(n, i ->
                        bookingService.create(request(f.photo90.getId(), slot, i), LocaleCode.KO));

                long rows = bookingRepository.count();
                report("동일슬롯", n, rep, run, rows);

                // 기대와 다르면 원문 로그를 전부 남깁니다.
                if (run.success.get() != 1 || run.deadlock.get() != 0 || rows != 1) {
                    dumpAnomaly("동일슬롯", n, rep, run, rows);
                }
            }
        }
    }

    // =====================================================================
    //  실험 2 — 겹치지 않는 슬롯 스케일
    //  N 개의 서로 겹치지 않는 슬롯에 동시에 몰립니다.
    //  같은 날짜 락을 공유하므로 직렬화 대기는 정상이지만, 전원 성공해야 합니다.
    //
    //  ⚠ 30분 상품을 쓰는 이유
    //    영업시간이 10:00~20:00(600분)이라 90분 상품으로는 20슬롯이 들어가지 않습니다.
    //    30분 × 20 = 600분으로 정확히 하루에 맞춥니다(10:00 ~ 19:30 시작).
    //    buffer-min=0, slot-step-min=30 이라 격자에도 정확히 정렬됩니다.
    // =====================================================================
    private void experiment2() throws Exception {
        for (int n : new int[]{5, 20}) {
            for (int rep = 1; rep <= REPEAT; rep++) {
                Fixture f = resetAndSeed();

                Run run = runConcurrently(n, i -> {
                    LocalDateTime slot = f.date.atTime(10, 0).plusMinutes(30L * i);
                    bookingService.create(request(f.photo30.getId(), slot, i), LocaleCode.KO);
                });

                long rows = bookingRepository.count();
                report("비겹침", n, rep, run, rows);

                if (run.success.get() != n || run.deadlock.get() != 0 || rows != n) {
                    dumpAnomaly("비겹침", n, rep, run, rows);
                }
            }
        }
    }

    // =====================================================================
    //  실험 3 — 취소 후 재예약
    //  예약 → 취소로 slot_key 가 NULL 이 된 뒤, 같은 슬롯에 10건이 동시에 몰립니다.
    //  기대: 성공 1. (UNIQUE 인덱스가 NULL 중복을 허용하므로 재예약이 가능해야 하고,
    //        그럼에도 동시 요청 중 1건만 통과해야 합니다)
    // =====================================================================
    private void experiment3() throws Exception {
        int n = 10;
        for (int rep = 1; rep <= REPEAT; rep++) {
            Fixture f = resetAndSeed();
            LocalDateTime slot = f.date.atTime(16, 0);

            var first = bookingService.create(request(f.photo90.getId(), slot, 0), LocaleCode.KO);
            Long firstId = bookingRepository.findByBookingCode(first.bookingCode())
                    .orElseThrow().getId();
            bookingService.cancelByAdmin(firstId, "부하 실험 취소");

            Run run = runConcurrently(n, i ->
                    bookingService.create(request(f.photo90.getId(), slot, i + 1), LocaleCode.KO));

            // 취소본(1) + 새 예약(1) = 2행이 정상입니다.
            long active = bookingRepository.findAll().stream()
                    .filter(b -> b.getStatus() != BookingStatus.CANCELED)
                    .count();
            long rows = bookingRepository.count();

            report("취소후재예약", n, rep, run, active);
            System.out.println("DETAIL|취소후재예약|rep=" + rep
                    + "|전체행=" + rows + "|활성=" + active
                    + "|취소본slotKey=" + bookingRepository.findById(firstId)
                            .map(b -> String.valueOf(b.getSlotKey())).orElse("삭제됨"));

            if (run.success.get() != 1 || active != 1) {
                dumpAnomaly("취소후재예약", n, rep, run, active);
            }
        }
    }

    // ===================== 공통 =====================

    private record Fixture(Product photo90, Product photo30, LocalDate date) {}

    /**
     * 시나리오마다 DB 를 완전히 비우고 다시 심습니다.
     * 앞 회차가 남긴 예약이 다음 회차의 겹침 검사에 걸리면 측정이 오염됩니다.
     */
    private Fixture resetAndSeed() {
        bookingRepository.deleteAllInBatch();
        dayLockRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        availabilityRepository.deleteAllInBatch();

        Product photo90 = productRepository.save(product("부하실험 90분", 90));
        Product photo30 = productRepository.save(product("부하실험 30분", 30));

        LocalDate date = LocalDate.now().plusDays(7);
        availabilityRepository.save(Availability.builder()
                .dayOfWeek(date.getDayOfWeek())
                .openTime(LocalTime.of(10, 0))
                .closeTime(LocalTime.of(20, 0))
                .active(true)
                .build());

        return new Fixture(photo90, photo30, date);
    }

    private Product product(String name, int durationMin) {
        return Product.builder()
                .type(ProductType.PHOTO)
                .nameKo(name)
                .durationMin(durationMin)
                .price(BigDecimal.valueOf(100_000))
                .currency("KRW")
                .active(true)
                .bookable(true)
                .sortOrder(1)
                .build();
    }

    private BookingCreateRequest request(Long productId, LocalDateTime startAt, int index) {
        return new BookingCreateRequest(
                productId, startAt,
                "부하" + index,
                "010-0000-000" + (index % 10),
                "load" + index + "@example.com",
                null, true, List.of(), null);
    }

    private static final class Run {
        final AtomicInteger success = new AtomicInteger();
        final AtomicInteger failure = new AtomicInteger();
        /** SQLState 40001 — InnoDB 가 데드락을 감지해 트랜잭션을 롤백한 건수 */
        final AtomicInteger deadlock = new AtomicInteger();
        final Queue<String> errors = new ConcurrentLinkedQueue<>();
        long elapsedMs;
    }

    private Run runConcurrently(int threadCount, ThrowingTask task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        Run run = new Run();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    ready.await();          // 전원이 출발선에 설 때까지 대기
                    task.run(index);
                    run.success.incrementAndGet();
                } catch (Exception e) {
                    run.failure.incrementAndGet();
                    if (isDeadlock(e)) run.deadlock.incrementAndGet();
                    run.errors.add(describe(e));
                } finally {
                    done.countDown();
                }
            });
        }

        long start = System.nanoTime();
        ready.countDown();                  // 출발 신호
        boolean finished = done.await(180, TimeUnit.SECONDS);
        run.elapsedMs = (System.nanoTime() - start) / 1_000_000;
        executor.shutdownNow();

        assertThat(finished)
                .as("180초 안에 모든 스레드가 끝나야 합니다 (교착 의심)")
                .isTrue();
        return run;
    }

    /**
     * 예외 사슬을 끝까지 훑어 SQLState 40001 을 찾습니다.
     *
     * Spring 이 DeadlockLoserDataAccessException 으로 바꿔주기도 하지만,
     * 래핑 단계가 버전에 따라 달라져 클래스 이름으로 판단하면 놓칩니다.
     * SQLState 는 드라이버가 직접 채우는 값이라 가장 확실합니다.
     */
    private boolean isDeadlock(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof SQLException sql && "40001".equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private String describe(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String state = "";
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof SQLException sql) { state = " [SQLState=" + sql.getSQLState() + "]"; break; }
        }
        return e.getClass().getSimpleName() + " / " + root.getClass().getSimpleName()
                + state + ": " + root.getMessage();
    }

    private void report(String scenario, int n, int rep, Run run, long rows) {
        System.out.printf("RESULT|%s|%d|%d|%d|%d|%d|%d|%d%n",
                scenario, n, rep,
                run.success.get(), run.failure.get(), run.deadlock.get(), rows, run.elapsedMs);

        // ★ 실패 "건수"만으로는 부족합니다.
        //   "이미 예약된 시간"(정상 거절)과 "락 대기 타임아웃"(과부하)은
        //   둘 다 실패로 세지지만 사용자에게 보이는 것도, 대응도 완전히 다릅니다.
        //   전자는 409, 후자는 429 입니다. 그래서 사유별로 나눠 남깁니다.
        List<String> all = new ArrayList<>(run.errors);
        all.stream().map(this::classify).distinct().sorted().forEach(kind ->
                System.out.println("REASON|" + scenario + "|" + n + "|" + rep + "|" + kind + "|"
                        + all.stream().filter(m -> classify(m).equals(kind)).count()));
    }

    /** 실패 사유를 사용자에게 나가는 결과 기준으로 묶습니다. */
    private String classify(String message) {
        if (message.contains("SlotAlreadyTakenException")) return "슬롯 선점(409)";
        if (message.contains("[SQLState=40001]")) return "데드락(40001)";
        if (message.contains("CannotAcquireLock") || message.contains("LockAcquisition")
                || message.contains("Lock wait timeout")) return "락 대기 타임아웃(429)";
        if (message.contains("SQLTransientConnection") || message.contains("Connection is not available"))
            return "커넥션 풀 고갈";
        return "기타";
    }

    /** 기대와 다른 회차는 실패 사유를 전부(중복 포함 개수까지) 남깁니다. */
    private void dumpAnomaly(String scenario, int n, int rep, Run run, long rows) {
        System.out.println("ANOMALY-BEGIN|" + scenario + "|N=" + n + "|rep=" + rep
                + "|success=" + run.success.get() + "|deadlock=" + run.deadlock.get()
                + "|dbRows=" + rows);
        List<String> all = new ArrayList<>(run.errors);
        all.stream().distinct().forEach(msg -> {
            long count = all.stream().filter(msg::equals).count();
            System.out.println("ANOMALY-LOG| x" + count + "  " + msg);
        });
        System.out.println("ANOMALY-END");
    }

    @FunctionalInterface
    private interface ThrowingTask {
        void run(int index) throws Exception;
    }
}
