package com.monsterhouse.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * available=false 인 슬롯도 내려줍니다.
 * 프론트에서 "마감" 표시를 해야 네이버 예약처럼 보이고, 사용자가 다른 시간을 고르기 쉽습니다.
 */
public record SlotResponse(

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime startAt,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime endAt,

        boolean available
) {

    public static SlotResponse of(LocalDateTime startAt, LocalDateTime endAt, boolean available) {
        return new SlotResponse(startAt, endAt, available);
    }
}