package com.monsterhouse.admin.security;

import com.monsterhouse.common.config.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookie {
    private static final String PATH = "/api/admin/auth";
    private final JwtProperties properties;
    public ResponseCookie create(String rawToken){
        return base(rawToken)
                .maxAge(Duration.ofDays(properties.refreshExpDays()))
                .build();
    }
    public ResponseCookie expire(){
        return base("").maxAge(Duration.ZERO).build();
    }
    public Optional<String> read(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        if(cookies == null){
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> properties.refreshCookieName().equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }
    private ResponseCookie.ResponseCookieBuilder base(String value){
        return ResponseCookie.from(properties.refreshCookieName(), value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite(properties.cookieSameSite())
                .path(PATH);
    }
}
