package com.monsterhouse.booking.service;

import com.monsterhouse.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** MH-20260804-7K2Q 형태. PK 노출 방지 + 고객 문의 시 구두 전달이 쉬운 길이. */
@Component
@RequiredArgsConstructor
public class BookingCodeGenerator {

    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final DateTimeFormatter DATE_PART = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int SUFFIX_LENGTH = 4;
    private static final int MAX_ATTEMPTS = 10;
    private final SecureRandom random = new SecureRandom();
    private final BookingRepository bookingRepository;
    public String generate(LocalDate date) {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String code = "MH-" + date.format(DATE_PART) + "-" + randomSuffix();
            if (!bookingRepository.existsByBookingCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("예약 번호 생성에 실패했습니다. 시도 횟수 초과.");
    }
    private String randomSuffix() {
        StringBuilder sb = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}