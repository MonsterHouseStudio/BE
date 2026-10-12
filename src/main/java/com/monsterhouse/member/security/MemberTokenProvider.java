package com.monsterhouse.member.security;

import com.monsterhouse.common.config.JwtProperties;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import com.monsterhouse.member.entity.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * 고객 회원용 액세스 토큰. 관리자 토큰과 같은 서명키를 쓰되
 * typ="member" 로 구분합니다(관리자 필터가 회원 토큰을 관리자 토큰으로 오인하지 않음).
 * 리프레시 없이 7일짜리 단일 토큰(프론트 localStorage 보관) — 고객 로그인 MVP.
 */
@Component
public class MemberTokenProvider {

    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_MEMBER = "member";
    private static final long EXP_SECONDS = 7L * 24 * 60 * 60; // 7일

    private final SecretKey key;
    private final String issuer;

    public MemberTokenProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.issuer = properties.issuer();
    }

    public String createToken(Member member) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(member.getId()))
                .claim(CLAIM_TYPE, TYPE_MEMBER)
                .claim("email", member.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(EXP_SECONDS)))
                .signWith(key)
                .compact();
    }

    public long expiresInSeconds() {
        return EXP_SECONDS;
    }

    /** 유효한 회원 토큰이면 memberId 반환, 아니면 UNAUTHORIZED. */
    public Long parseMemberId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!TYPE_MEMBER.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }
            return Long.valueOf(claims.getSubject());
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }
}
