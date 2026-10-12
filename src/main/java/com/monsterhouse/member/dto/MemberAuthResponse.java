package com.monsterhouse.member.dto;

/** 회원가입/로그인 응답 — 액세스 토큰 + 회원 정보. */
public record MemberAuthResponse(
        String accessToken,
        long expiresInSeconds,
        MemberResponse member
) {
}
