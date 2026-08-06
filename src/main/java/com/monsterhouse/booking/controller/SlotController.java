package com.monsterhouse.booking.controller;

import com.monsterhouse.booking.dto.response.DailySlotsResponse;
import com.monsterhouse.booking.service.SlotService;
import com.monsterhouse.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}")
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;

    /** 시간 선택 화면 — 특정 날짜의 슬롯 목록 */
    @GetMapping("/slots")
    public ApiResponse<DailySlotsResponse> slots(
            @PathVariable Long productId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ApiResponse.ok(slotService.getDailySlots(productId, date));
    }

    /** 달력 화면 — 예약 가능한 날짜만 표시 */
    @GetMapping("/bookable-dates")
    public ApiResponse<List<LocalDate>> bookableDates(
            @PathVariable Long productId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ApiResponse.ok(slotService.getBookableDates(productId, from, to));
    }
}