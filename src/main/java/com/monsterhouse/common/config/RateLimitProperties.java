package com.monsterhouse.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int capacity,
        int windowSeconds
) {
}
