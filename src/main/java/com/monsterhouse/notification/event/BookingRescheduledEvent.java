package com.monsterhouse.notification.event;

import java.time.LocalDateTime;

public record BookingRescheduledEvent(
        Long bookingId,
        LocalDateTime previousStartAt,
        LocalDateTime currentStartAt
) {
}
