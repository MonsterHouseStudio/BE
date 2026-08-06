package com.monsterhouse.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * app.line.*
 * ※ LINE Notify 는 2025-03-31 종료. Messaging API 의 push 엔드포인트를 씁니다.
 * ※ 무료 200통/월은 "수신자 수 × 메시지 수"로 차감되므로 adminUserId 는 그룹이 아닌 담당자 1명 권장.
 */
@ConfigurationProperties(prefix = "app.line")
public record LineProperties(
        boolean enabled,
        String channelAccessToken,
        String adminUserId,
        String pushUrl,
        int connectTimeoutMs,
        int readTimeoutMs,
        int maxRetry
) {
}
