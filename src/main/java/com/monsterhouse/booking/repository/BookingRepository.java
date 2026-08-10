package com.monsterhouse.booking.repository;

import com.monsterhouse.booking.entity.Booking;
import com.monsterhouse.booking.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, BookingSearchRepository{

    /**
     * 개인정보 보유기간 경과분 파기 (기획서 §9).
     *
     * 조건이 두 겹인 이유:
     *   - 완료/취소된 건만 대상입니다. 진행 중인 예약을 날짜만 보고 지우면 운영이 마비됩니다.
     *   - 기준 시각은 "촬영 시각(startAt)"입니다. 신청일이 아니라 실제 촬영일로부터 세야
     *     방침에 적은 "촬영 후 1년"과 코드가 같은 의미가 됩니다.
     */
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("""
            delete from Booking b
            where b.startAt < :threshold
              and b.status in (com.monsterhouse.booking.entity.BookingStatus.COMPLETED,
                               com.monsterhouse.booking.entity.BookingStatus.CANCELED)
            """)
    int deleteFinishedBefore(@Param("threshold") LocalDateTime threshold);

    /**
     * ★ 동시성 방어 2층 — 시간대 겹침 검사 (촬영팀 공유 자원 가정)
     *
     * 겹침 조건: A.start < B.end AND A.end > B.start
     * 등호를 넣지 않는 것이 핵심입니다. 10:00~11:30 과 11:30~13:00 은 겹치지 않습니다.
     *
     * ※ 반드시 날짜 락을 잡은 뒤 호출해야 합니다.
     *    락 없이 호출하면 두 트랜잭션이 동시에 "없음"을 보고 둘 다 INSERT 합니다.
     */
    @Query("""
            select case when count(b) > 0 then true else false end
            from Booking b
            where b.status in :statuses
              and b.startAt < :endAt
              and b.endAt > :startAt
            """)
    boolean existsOverlap(@Param("startAt") LocalDateTime startAt,
                          @Param("endAt") LocalDateTime endAt,
                          @Param("statuses") Collection<BookingStatus> statuses);

    /** sharedResource=false 인 경우: 같은 상품 안에서만 겹침을 봅니다. */
    @Query("""
            select case when count(b) > 0 then true else false end
            from Booking b
            where b.product.id = :productId
              and b.status in :statuses
              and b.startAt < :endAt
              and b.endAt > :startAt
            """)
    boolean existsOverlapByProduct(@Param("productId") Long productId,
                                   @Param("startAt") LocalDateTime startAt,
                                   @Param("endAt") LocalDateTime endAt,
                                   @Param("statuses") Collection<BookingStatus> statuses);
    /**
     * ★ 예약 변경용 겹침 검사 — 자기 자신을 반드시 제외합니다.
     *
     * 이게 없으면 10:00 예약을 10:30 으로 옮기는 것처럼
     * "옛 시간과 새 시간이 겹치는" 이동이 100% 실패합니다.
     * 자기 자신을 겹치는 예약으로 세어버리기 때문입니다.
     */
    @Query("""
            select case when count(b) > 0 then true else false end
            from Booking b
            where b.id <> :excludeId
              and b.status in :statuses
              and b.startAt < :endAt
              and b.endAt > :startAt
            """)
    boolean existsOverlapExcluding(@Param("excludeId") Long excludeId,
                                   @Param("startAt") LocalDateTime startAt,
                                   @Param("endAt") LocalDateTime endAt,
                                   @Param("statuses") Collection<BookingStatus> statuses);

    /** sharedResource=false 인 경우 */
    @Query("""
            select case when count(b) > 0 then true else false end
            from Booking b
            where b.id <> :excludeId
              and b.product.id = :productId
              and b.status in :statuses
              and b.startAt < :endAt
              and b.endAt > :startAt
            """)
    boolean existsOverlapByProductExcluding(@Param("excludeId") Long excludeId,
                                            @Param("productId") Long productId,
                                            @Param("startAt") LocalDateTime startAt,
                                            @Param("endAt") LocalDateTime endAt,
                                            @Param("statuses") Collection<BookingStatus> statuses);
    /**
     * 슬롯 목록 계산용 — 해당 날짜에 걸쳐 있는 활성 예약.
     * startAt 이 전날이고 endAt 이 오늘로 넘어오는 경우까지 잡기 위해 겹침 조건을 씁니다.
     */
    @Query("""
            select b from Booking b
            where b.status in :statuses
              and b.startAt < :dayEnd
              and b.endAt > :dayStart
            order by b.startAt asc
            """)
    List<Booking> findOccupying(@Param("dayStart") LocalDateTime dayStart,
                                @Param("dayEnd") LocalDateTime dayEnd,
                                @Param("statuses") Collection<BookingStatus> statuses);

    Optional<Booking> findByBookingCode(String bookingCode);

    boolean existsByBookingCode(String bookingCode);

    @Query("""
            select b from Booking b
            join fetch b.product
            where b.startAt >= :from and b.startAt < :to
            order by b.startAt asc
            """)
    List<Booking> findAllByPeriodWithProduct(@Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);

    Page<Booking> findAllByStatusOrderByStartAtDesc(BookingStatus status, Pageable pageable);

    Page<Booking> findAllByOrderByStartAtDesc(Pageable pageable);
    @Query("""
        select case when count(b) > 0 then true else false end
        from Booking b
        where b.product.id = :productId and b.status in :statuses
        """)
    boolean existsByProductIdAndStatusIn(@Param("productId") Long productId,
                                         @Param("statuses") Collection<BookingStatus> statuses);

    long countByStartAtBetween(LocalDateTime from, LocalDateTime to);
    long countByStatus(BookingStatus status);

    /**
     * 기간 내 확정·완료 예약의 결제 금액 합계.
     *
     * ⚠ b.product.price 가 아니라 b.totalPrice 를 더합니다. (제가 처음 드린 코드를 수정했습니다)
     *   - 상품가만 더하면 옵션(보정본 추가 등) 금액이 통째로 빠집니다
     *   - 상품 가격을 나중에 올리면 과거 매출까지 소급해서 바뀝니다
     *   totalPrice 는 예약 시점에 고정된 스냅샷이라 둘 다 해결됩니다.
     *
     * 취소 건을 빼지 않으면 "예상 매출"이 부풀려지고 그 숫자로 운영 판단을 하게 됩니다.
     * coalesce 가 없으면 대상이 없을 때 null 이 돌아와 NPE 가 납니다.
     */
    @Query("""
            select coalesce(sum(b.totalPrice), 0)
            from Booking b
            where b.startAt >= :from and b.startAt < :to
              and b.status in :statuses
            """)
    BigDecimal sumPriceBetween(@Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to,
                                    @Param("statuses") Collection<BookingStatus> statuses);
}