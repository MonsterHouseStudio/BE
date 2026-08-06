package com.monsterhouse.booking.repository;

import com.monsterhouse.booking.entity.BookingDayLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.QueryHint;

import java.time.LocalDate;
import java.util.Optional;

public interface BookingDayLockRepository extends JpaRepository<BookingDayLock, LocalDate> {

    /**
     * ★ SELECT ... FOR UPDATE
     * jakarta.persistence.lock.timeout 을 주지 않으면 MySQL 기본
     * innodb_lock_wait_timeout(docker-compose 에서 10초로 설정)을 따릅니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "10000")})
    @Query("select l from BookingDayLock l where l.lockDate = :date")
    Optional<BookingDayLock> findForUpdate(@Param("date") LocalDate date);

    /**
     * 락 행이 없으면 만듭니다.
     * INSERT IGNORE 를 쓰는 이유: 두 트랜잭션이 동시에 같은 날짜 행을 만들려 할 때
     * 하나는 조용히 무시되고, 그 대기 자체가 직렬화 역할을 합니다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "INSERT IGNORE INTO booking_day_lock (lock_date) VALUES (:date)",
            nativeQuery = true)
    int insertIgnore(@Param("date") LocalDate date);
}