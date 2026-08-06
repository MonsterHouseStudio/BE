package com.monsterhouse.common.util;

import com.monsterhouse.common.enums.LocaleCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 기획서 §3.3 — KST/JST 는 같은 UTC+9 라 시각 변환은 불필요하지만
 * 표기 형식이 다릅니다. 2026년 8월 4일 / 2026年8月4日
 */
public final class DateTimeFormatUtil {

    private static final DateTimeFormatter KO_DATE =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일(E)", LocaleCode.KO.getLocale());
    private static final DateTimeFormatter JA_DATE =
            DateTimeFormatter.ofPattern("yyyy年M月d日(E)", LocaleCode.JA.getLocale());
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm");

    private DateTimeFormatUtil() {
    }

    public static String date(LocalDate date, LocaleCode locale) {
        return date.format(locale == LocaleCode.JA ? JA_DATE : KO_DATE);
    }

    public static String time(LocalTime time) {
        return time.format(TIME);
    }

    public static String dateTime(LocalDateTime dateTime, LocaleCode locale) {
        return date(dateTime.toLocalDate(), locale) + " " + time(dateTime.toLocalTime());
    }

    /** 예약 확인 메일용: 2026년 8월 4일(화) 14:00 ~ 15:30 */
    public static String range(LocalDateTime start, LocalDateTime end, LocaleCode locale) {
        return dateTime(start, locale) + " ~ " + time(end.toLocalTime());
    }
}