package com.monsterhouse.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * ★ 예약 동시성 방어 1층 — 날짜 단위 직렬화용 락 행.
 *
 * 왜 필요한가:
 *   슬롯을 DB 에 미리 INSERT 해두지 않고 영업시간에서 동적 계산하므로,
 *   "슬롯 행에 SELECT ... FOR UPDATE" 를 걸 대상이 존재하지 않습니다.
 *   그래서 날짜 1행을 잠가 [겹침 검사 → INSERT] 임계구역을 보호합니다.
 *
 * 왜 날짜 단위인가:
 *   슬롯 단위로 잠그면 겹치는 옆 슬롯을 못 막습니다(90분 상품이 30분 그리드를 넘나듦).
 *   날짜 단위면 그날의 모든 예약 요청이 직렬화되어 겹침 검사가 항상 정확합니다.
 *   촬영 예약은 하루 수 건 규모이므로 이 정도 직렬화는 성능 문제가 되지 않습니다.
 *
 * 트랜잭션 커밋과 동시에 자동 해제되므로 GET_LOCK() 같은 명시적 해제가 필요 없습니다.
 */
@Getter
@Entity
@Table(name = "booking_day_lock")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingDayLock {

    @Id
    @Column(name = "lock_date", nullable = false)
    private LocalDate lockDate;

    public BookingDayLock(LocalDate lockDate) {
        this.lockDate = lockDate;
    }
}