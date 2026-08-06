package com.monsterhouse.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.monsterhouse.booking.entity.AvailabilityOverride;
import com.monsterhouse.booking.entity.OverrideType;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilityOverrideResponse(
        Long id,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate date,

        OverrideType type,

        @JsonFormat(pattern = "HH:mm")
        LocalTime openTime,

        @JsonFormat(pattern = "HH:mm")
        LocalTime closeTime,

        String memo
) {

    public static AvailabilityOverrideResponse of(AvailabilityOverride override) {
        return new AvailabilityOverrideResponse(
                override.getId(),
                override.getOverrideDate(),
                override.getType(),
                override.getOpenTime(),
                override.getCloseTime(),
                override.getMemo()
        );
    }
}