package com.monsterhouse.booking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookingCancelRequest(
        @NotBlank(message = "{valid.email.required}")
        @Email(message = "{valid.email.format}")
        String email,
        @Size(max = 300)
        String reason
) {
}
