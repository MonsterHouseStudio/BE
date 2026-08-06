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
        ALLOWED.put(CONFIRMED, EnumSet.of(COMPLETED, CANCELED));
        ALLOWED.put(COMPLETED, EnumSet.noneOf(BookingStatus.class));
        ALLOWED.put(CANCELED, EnumSet.noneOf(BookingStatus.class));
    }

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