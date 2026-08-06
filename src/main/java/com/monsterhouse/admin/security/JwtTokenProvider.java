package com.monsterhouse.admin.security;

import com.monsterhouse.admin.entity.AdminRole;
import com.monsterhouse.admin.entity.AdminUser;
import com.monsterhouse.common.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * 액세스 토큰 전용. 리프레시 토큰은 JWT 가 아니라
 * 불투명 랜덤 문자열 + DB 해시 저장 방식입니다(AdminAuthService 참고).
 *
 * 왜 리프레시를 JWT 로 안 만드는가:
 *   JWT 는 서명만 맞으면 서버가 상태를 안 봐도 유효합니다.
 *   즉 "로그아웃"이나 "탈취된 세션 강제 종료"를 구현할 수 없습니다.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtTokenProvider(JwtProperties properties) {
        byte[] secretBytes = properties.secret().getBytes(StandardCharsets.UTF_8);

        // HS256 은 최소 256bit(32바이트) 키를 요구합니다.
        // 짧으면 기동 시점에 터뜨려서, 운영 중에 발견되는 일이 없게 합니다.
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret 은 최소 32바이트여야 합니다. 현재 " + secretBytes.length + "바이트");
        }

        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.properties = properties;
    }

    public String createAccessToken(AdminUser admin) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.accessExpMinutes() * 60);

        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(admin.getId()))
                .claim(CLAIM_ROLE, admin.getRole().name())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim("username", admin.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public long accessTokenExpiresInSeconds() {
        return properties.accessExpMinutes() * 60;
    }

    /**
     * 검증 실패 사유를 구분해서 던집니다.
     * 프론트가 "만료 → 조용히 refresh" 와 "위조 → 로그인 화면" 을 나눠 처리해야 하기 때문입니다.
     */
    public AdminPrincipal parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new InvalidTokenException("액세스 토큰이 아닙니다.");
            }

            return new AdminPrincipal(
                    Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    AdminRole.valueOf(claims.get(CLAIM_ROLE, String.class))
            );

        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException(e.getMessage());
        }
    }

    // 필터에서 401 사유를 구분하기 위한 내부 예외입니다.
    public static class TokenExpiredException extends RuntimeException {
        public TokenExpiredException() {
            super("access token expired");
        }
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
}