package com.monsterhouse.booking.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AdminBookingRescheduleRequest(
        @NotNull
        LocalDateTime startAt
) {
}
