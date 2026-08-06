package com.monsterhouse.booking.controller;

import com.monsterhouse.booking.dto.request.AvailabilityOverrideRequest;
import com.monsterhouse.booking.dto.request.AvailabilityUpsertRequest;
import com.monsterhouse.booking.dto.response.AvailabilityOverrideResponse;
import com.monsterhouse.booking.dto.response.AvailabilityResponse;
import com.monsterhouse.booking.service.AvailabilityService;
import com.monsterhouse.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/availability")
@RequiredArgsConstructor
public class AdminAvailabilityController {

    private final AvailabilityService availabilityService;

    /** 요일별 기본 영업시간 (항상 7개) */
    @GetMapping
    public ApiResponse<List<AvailabilityResponse>> week() {
        return ApiResponse.ok(availabilityService.findWeek());
    }

    @PutMapping
    public ApiResponse<AvailabilityResponse> upsert(
            @Valid @RequestBody AvailabilityUpsertRequest request) {
        return ApiResponse.ok(availabilityService.upsert(request));
    }

    /** 날짜 예외 — 달력 화면에서 한 달치를 한 번에 가져갑니다. */
    @GetMapping("/overrides")
    public ApiResponse<List<AvailabilityOverrideResponse>> overrides(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(availabilityService.findOverrides(from, to));
    }

    @PutMapping("/overrides")
    public ApiResponse<AvailabilityOverrideResponse> saveOverride(
            @Valid @RequestBody AvailabilityOverrideRequest request) {
        return ApiResponse.ok(availabilityService.saveOverride(request));
    }

    @DeleteMapping("/overrides/{overrideId}")
    public ApiResponse<Void> deleteOverride(@PathVariable Long overrideId) {
        availabilityService.deleteOverride(overrideId);
        return ApiResponse.ok();
    }
}