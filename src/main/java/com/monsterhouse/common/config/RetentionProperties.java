package com.monsterhouse.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * app.retention.*
 * 개인정보 보유기간 (기획서 §9 — 보유기간 명시·자동 파기 배치)
 */
@ConfigurationProperties(prefix = "app.retention")
public record RetentionProperties(
        boolean enabled,
        int bookingDays,
        int inquiryDays,
        String cron
) {
}
