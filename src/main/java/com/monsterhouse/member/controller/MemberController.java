package com.monsterhouse.member.controller;

import com.monsterhouse.booking.dto.response.BookingResponse;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.member.dto.MemberAuthResponse;
import com.monsterhouse.member.dto.MemberLoginRequest;
import com.monsterhouse.member.dto.MemberResponse;
import com.monsterhouse.member.dto.MemberSignupRequest;
import com.monsterhouse.member.security.MemberTokenProvider;
import com.monsterhouse.member.service.MemberAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 고객 회원 API.
 * signup/login 은 공개(SecurityConfig 에 POST 허용 추가).
 * me/me-bookings 는 GET permitAll 이지만 아래에서 회원 토큰을 직접 검증합니다.
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberAuthService memberAuthService;
    private final MemberTokenProvider tokenProvider;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberAuthResponse> signup(@Valid @RequestBody MemberSignupRequest request) {
        return ApiResponse.ok(memberAuthService.signup(request));
    }

    @PostMapping("/login")
    public ApiResponse<MemberAuthResponse> login(@Valid @RequestBody MemberLoginRequest request) {
        return ApiResponse.ok(memberAuthService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<MemberResponse> me(HttpServletRequest http) {
        return ApiResponse.ok(memberAuthService.getMe(requireMemberId(http)));
    }

    @GetMapping("/me/bookings")
    public ApiResponse<List<BookingResponse>> myBookings(HttpServletRequest http) {
        LocaleCode locale = LocaleCode.from(LocaleContextHolder.getLocale());
        return ApiResponse.ok(memberAuthService.getMyBookings(requireMemberId(http), locale));
    }

    /** Authorization: Bearer <회원토큰> 에서 memberId 를 꺼냅니다. 없거나 유효하지 않으면 401. */
    private Long requireMemberId(HttpServletRequest http) {
        String header = http.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return tokenProvider.parseMemberId(header.substring(7).trim());
    }
}
