package com.monsterhouse.booking.entity;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 기획서 §4.2 상태 흐름
 *   REQUESTED → CONFIRMED → COMPLETED
 *        ↓          ↓
 *     CANCELED   CANCELED
 */
public enum BookingStatus {

    REQUESTED,
    CONFIRMED,
    COMPLETED,
    CANCELED;

    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED =
            new EnumMap<>(BookingStatus.class);

    static {
        ALLOWED.put(REQUESTED, EnumSet.of(CONFIRMED, CANCELED));

        // ★ REQUESTED 가 추가됐습니다.
        //   확정된 예약을 고객이 스스로 다른 시간으로 옮기면 확정을 되돌립니다.
        //   사장님이 그 시간에 맞춰 잡아둔 일정이 있으므로 새 시간은 재확인이 필요합니다.
        //   이 전이는 Booking.revertToRequested() 에서만 사용합니다.
        ALLOWED.put(CONFIRMED, EnumSet.of(COMPLETED, CANCELED, REQUESTED));

        ALLOWED.put(COMPLETED, EnumSet.noneOf(BookingStatus.class));
        ALLOWED.put(CANCELED, EnumSet.noneOf(BookingStatus.class));
    }
    public boolean canReschedule() {return this == REQUESTED || this == CONFIRMED;}
    public boolean canTransitionTo(BookingStatus next) {
        return ALLOWED.get(this).contains(next);
    }

    /** 슬롯을 점유하는 상태인가? 겹침 검사 대상. */
    public boolean occupiesSlot() {
        return this == REQUESTED || this == CONFIRMED || this == COMPLETED;
    }

    public static Set<BookingStatus> occupyingStatuses() {
        return EnumSet.of(REQUESTED, CONFIRMED, COMPLETED);
    }
}