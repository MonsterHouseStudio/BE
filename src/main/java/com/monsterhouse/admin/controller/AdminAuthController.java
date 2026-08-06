package com.monsterhouse.admin.controller;

import com.monsterhouse.admin.dto.AdminProfileResponse;
import com.monsterhouse.admin.dto.LoginRequest;
import com.monsterhouse.admin.dto.TokenResponse;
import com.monsterhouse.admin.security.AdminPrincipal;
import com.monsterhouse.admin.security.RefreshTokenCookie;
import com.monsterhouse.admin.service.AdminAuthService;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import com.monsterhouse.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService authService;
    private final RefreshTokenCookie refreshCookie;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        var result = authService.login(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.create(result.rawRefreshToken()).toString())
                .body(ApiResponse.ok(result.tokens()));
    }

    /**
     * 액세스 토큰이 만료됐을 때 프론트가 조용히 호출합니다.
     * 리프레시 토큰은 요청 본문이 아니라 쿠키에서 읽습니다.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(HttpServletRequest request) {
        String rawToken = refreshCookie.read(request)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

        var result = authService.refresh(rawToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.create(result.rawRefreshToken()).toString())
                .body(ApiResponse.ok(result.tokens()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        refreshCookie.read(request).ifPresent(authService::logout);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.expire().toString())
                .body(ApiResponse.ok());
    }

    /** 새로고침 시 프론트가 세션 유효성을 확인하는 용도 */
    @GetMapping("/me")
    public ApiResponse<AdminProfileResponse> me(@AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.ok(AdminProfileResponse.of(authService.getById(principal.id())));
    }
}