package com.monsterhouse.booking.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.monsterhouse.booking.entity.OverrideType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilityOverrideRequest(

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate date,

        @NotNull
        OverrideType type,

        /** SPECIAL 일 때만 사용. HOLIDAY 면 무시됩니다. */
        @JsonFormat(pattern = "HH:mm")
        LocalTime openTime,

        @JsonFormat(pattern = "HH:mm")
        LocalTime closeTime,

        @Size(max = 200)
        String memo
) {
}