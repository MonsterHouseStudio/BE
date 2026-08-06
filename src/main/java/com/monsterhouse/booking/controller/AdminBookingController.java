package com.monsterhouse.booking.controller;

import com.monsterhouse.booking.dto.request.BookingSearchCondition;
import com.monsterhouse.booking.dto.response.AdminBookingResponse;
import com.monsterhouse.booking.dto.response.BookingResponse;
import com.monsterhouse.booking.entity.Booking;
import com.monsterhouse.booking.entity.BookingStatus;
import com.monsterhouse.booking.repository.BookingRepository;
import com.monsterhouse.booking.service.BookingService;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<AdminBookingResponse>> list(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var condition = new BookingSearchCondition(status, from, to, productId, keyword);
        Page<Booking> result = bookingRepository.search(condition, PageRequest.of(page, size));

        return ApiResponse.ok(PageResponse.of(result, AdminBookingResponse::of));
    }
    @GetMapping("/calendar")
    @Transactional(readOnly = true)
    public ApiResponse<List<BookingResponse>> calendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<BookingResponse> bookings = bookingRepository
                .findAllByPeriodWithProduct(from.atStartOfDay(), to.plusDays(1).atStartOfDay())
                .stream()
                .map(b -> BookingResponse.of(b, LocaleCode.KO))
                .toList();
        return ApiResponse.ok(bookings);
    }
    @PostMapping("/{bookingId}/confirm")
    public ApiResponse<BookingResponse> confirm(@PathVariable Long bookingId) {
        return ApiResponse.ok(bookingService.confirm(bookingId));
    }
    @PostMapping("/{bookingId}/complete")
    public ApiResponse<BookingResponse> complete(@PathVariable Long bookingId) {
        return ApiResponse.ok(bookingService.complete(bookingId));
    }
    @PostMapping("/{bookingId}/cancel")
    public ApiResponse<BookingResponse> cancel(@PathVariable Long bookingId, @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ApiResponse.ok(bookingService.cancelByAdmin(bookingId, reason));
    }
}