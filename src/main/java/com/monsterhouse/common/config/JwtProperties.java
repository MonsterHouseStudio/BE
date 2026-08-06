package com.monsterhouse.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties (
        String secret,
        long accessExpMinutes,
        long refreshExpDays,
        String issuer,
        String refreshCookieName,
        boolean cookieSecure,
        String cookieSameSite
){
}
