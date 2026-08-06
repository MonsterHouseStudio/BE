package com.monsterhouse.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record DailySlotsResponse(

        Long productId,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate date,

        /** 휴무일·영업시간 밖이면 false, slots 는 빈 배열 */
        boolean open,

        /** open=false 인 이유 코드 (HOLIDAY / CLOSED_DAY / OUT_OF_RANGE) */
        String closedReason,

        List<SlotResponse> slots
) {

    public static DailySlotsResponse open(Long productId, LocalDate date, List<SlotResponse> slots) {
        return new DailySlotsResponse(productId, date, true, null, slots);
    }

    public static DailySlotsResponse closed(Long productId, LocalDate date, String reason) {
        return new DailySlotsResponse(productId, date, false, reason, List.of());
    }
}