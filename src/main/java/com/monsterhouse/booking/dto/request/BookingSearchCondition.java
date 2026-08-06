package com.monsterhouse.booking.dto.request;

import com.monsterhouse.booking.entity.BookingStatus;

import java.time.LocalDate;

public record BookingSearchCondition(
        BookingStatus status,
        LocalDate from,
        LocalDate to,
        Long productId,
        String keyword
) {
}
