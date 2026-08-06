package com.monsterhouse.booking.controller;

import com.monsterhouse.booking.dto.request.BookingCancelRequest;
import com.monsterhouse.booking.dto.request.BookingCreateRequest;
import com.monsterhouse.booking.dto.response.BookingResponse;
import com.monsterhouse.booking.service.BookingService;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BookingResponse> create(@Valid @RequestBody BookingCreateRequest request){
        return ApiResponse.ok(bookingService.create(request, currentLocale()));
    }
    @GetMapping("/{bookingCode}")
    public ApiResponse<BookingResponse> detail(@PathVariable String bookingCode, @RequestParam String email){
        return ApiResponse.ok(bookingService.findByCode(bookingCode, email, currentLocale()));
    }
    @PostMapping("/{bookingCode}/cancel")
    public ApiResponse<BookingResponse> cancel(@PathVariable String bookingCode, @Valid @RequestBody BookingCancelRequest request){
        return ApiResponse.ok(bookingService.cancelByCustomer(bookingCode, request, currentLocale()));
    }
    private LocaleCode currentLocale(){
        return LocaleCode.from(LocaleContextHolder.getLocale());
    }
}
