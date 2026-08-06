package com.monsterhouse.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.monsterhouse.booking.entity.Availability;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record AvailabilityResponse(
        Long id,
        DayOfWeek dayOfWeek,

        @JsonFormat(pattern = "HH:mm")
        LocalTime openTime,

        @JsonFormat(pattern = "HH:mm")
        LocalTime closeTime,

        boolean active
) {

    public static AvailabilityResponse of(Availability availability) {
        return new AvailabilityResponse(
                availability.getId(),
                availability.getDayOfWeek(),
                availability.getOpenTime(),
                availability.getCloseTime(),
                availability.isActive()
        );
    }

    /** 아직 DB 에 행이 없는 요일. 관리자 화면에 7일을 항상 채워 보여주기 위함입니다. */
    public static AvailabilityResponse empty(DayOfWeek dayOfWeek) {
        return new AvailabilityResponse(null, dayOfWeek,
                LocalTime.of(10, 0), LocalTime.of(20, 0), false);
    }
}