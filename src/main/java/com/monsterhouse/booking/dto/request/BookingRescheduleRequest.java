package com.monsterhouse.booking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record BookingRescheduleRequest(
        @NotBlank(message = "{valid.email.required}")
        @Email(message = "{valid.email.format}")
        String email,
        @NotNull
        @Future
        LocalDateTime startAt
) {
}
