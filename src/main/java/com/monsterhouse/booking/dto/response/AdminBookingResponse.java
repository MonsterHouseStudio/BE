package com.monsterhouse.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.monsterhouse.booking.entity.Booking;
import com.monsterhouse.booking.entity.BookingStatus;
import com.monsterhouse.common.enums.LocaleCode;

import java.time.LocalDateTime;

public record AdminBookingResponse (
        Long id,
        String bookingCode,
        String productName,
        int durationMin,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime startAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime endAt,
        BookingStatus status,
        String name,
        String phone,
        String email,
        LocaleCode locale,
        String memo,
        java.math.BigDecimal totalPrice,
        java.util.List<String> optionSummary,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
){
    public static AdminBookingResponse of(Booking booking){
        return new AdminBookingResponse(
                booking.getId(),
                booking.getBookingCode(),
                booking.getProduct().name(LocaleCode.KO),
                booking.getProduct().getDurationMin(),
                booking.getStartAt(),
                booking.getEndAt(),
                booking.getStatus(),
                booking.getName(),
                booking.getPhone(),
                booking.getEmail(),
                booking.getLocale(),
                booking.getMemo(),
                booking.getTotalPrice(),
                booking.getOptions().stream()
                        .map(o -> o.getOptionName() + " x" + o.getQuantity())
                        .toList(),
                booking.getCreatedAt()
        );
    }
}
