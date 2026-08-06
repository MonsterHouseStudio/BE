package com.monsterhouse.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.monsterhouse.booking.entity.Booking;
import com.monsterhouse.booking.entity.BookingStatus;
import com.monsterhouse.common.enums.LocaleCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        String bookingCode,
        String productName,
        int durationMin,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime startAt,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime endAt,

        BookingStatus status,
        String name,
        String maskedPhone,
        String maskedEmail,
        String memo,

        /** 상품 기본가 (옵션 제외) */
        BigDecimal basePrice,
        /** 선택 옵션 내역 — 예약 시점 단가로 고정된 값입니다 */
        List<SelectedOption> options,
        /** 실제 청구 금액 */
        BigDecimal totalPrice,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

    public record SelectedOption(String name, BigDecimal unitPrice, int quantity, BigDecimal amount) {
    }

    /** 고객용 — 개인정보는 마스킹해서 내려보냅니다 (링크 유출 대비). */
    public static BookingResponse of(Booking booking, LocaleCode locale) {
        List<SelectedOption> options = booking.getOptions().stream()
                .map(o -> new SelectedOption(
                        o.getOptionName(), o.getUnitPrice(), o.getQuantity(), o.amount()))
                .toList();

        return new BookingResponse(
                booking.getBookingCode(),
                booking.getProduct().name(locale),
                booking.getProduct().getDurationMin(),
                booking.getStartAt(),
                booking.getEndAt(),
                booking.getStatus(),
                booking.getName(),
                mask(booking.getPhone(), 3),
                maskEmail(booking.getEmail()),
                booking.getMemo(),
                booking.getBasePrice(),
                options,
                booking.getTotalPrice(),
                booking.getCreatedAt()
        );
    }

    private static String mask(String value, int visible) {
        if (value == null || value.length() <= visible) {
            return value;
        }
        return value.substring(0, visible) + "*".repeat(value.length() - visible);
    }

    private static String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return email;
        }
        return email.charAt(0) + "*".repeat(at - 1) + email.substring(at);
    }
}
