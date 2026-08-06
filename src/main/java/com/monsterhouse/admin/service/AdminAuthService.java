package com.monsterhouse.admin.service;

import com.monsterhouse.admin.dto.LoginRequest;
import com.monsterhouse.admin.dto.TokenResponse;
import com.monsterhouse.admin.entity.AdminUser;
import com.monsterhouse.admin.entity.RefreshToken;
import com.monsterhouse.admin.repository.AdminUserRepository;
import com.monsterhouse.admin.repository.RefreshTokenRepository;
import com.monsterhouse.admin.security.JwtTokenProvider;
import com.monsterhouse.common.config.AdminSecurityProperties;
import com.monsterhouse.common.config.JwtProperties;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * 관리자 인증. 기획서 §9 (JWT + 리프레시 토큰, BCrypt, 로그인 시도 제한)
 *
 * 리프레시 토큰 회전(rotation):
 *   refresh 를 호출할 때마다 새 토큰을 발급하고 쓴 토큰은 즉시 폐기합니다.
 *   토큰 하나의 유효 기간이 사실상 "다음 refresh 까지"로 줄어듭니다.
 *
 * 재사용 탐지(reuse detection):
 *   이미 폐기된 토큰이 다시 들어왔다 = 누군가 복사본을 갖고 있다.
 *   정상 사용자와 공격자 중 누가 보냈는지 알 수 없으므로
 *   그 계정의 모든 세션을 끊고 재로그인을 강제합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int REFRESH_TOKEN_BYTES = 32;

    private final AdminUserRepository adminUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final AdminSecurityProperties adminProperties;

    /** 로그인 결과. 쿠키에 실을 원문 리프레시 토큰은 컨트롤러까지만 전달됩니다. */
    public record AuthResult(TokenResponse tokens, String rawRefreshToken) {
    }

    /**
     * ★ noRollbackFor 가 반드시 필요합니다.
     *
     * 기본 동작대로면 로그인 실패 시 BusinessException 을 던지는 순간 트랜잭션이 롤백되어
     * 바로 위에서 올린 failedLoginCount 증가분이 함께 사라집니다.
     * 그러면 카운터가 영원히 0 이라 계정이 절대 잠기지 않습니다(시도 제한 무력화).
     *
     * 실패 경로에서 더티한 상태는 "실패 카운터/잠금 시각" 뿐이고, 그건 커밋되어야 하는 값입니다.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResult login(LoginRequest request) {
        LocalDateTime now = LocalDateTime.now();

        AdminUser admin = adminUserRepository.findByUsername(request.username())
                .orElse(null);

        // 존재하지 않는 아이디와 틀린 비밀번호를 같은 응답으로 통일합니다.
        // 다르게 응답하면 아이디 존재 여부를 스캔당합니다(user enumeration).
        if (admin == null) {
            log.info("Login failed - unknown username");
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!admin.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (admin.isLocked(now)) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, admin.remainingLockMinutes(now));
        }

        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            admin.recordLoginFailure(
                    adminProperties.maxLoginAttempts(), adminProperties.lockMinutes(), now);

            log.info("Login failed - bad password. username={} locked={}",
                    admin.getUsername(), admin.isLocked(now));

            if (admin.isLocked(now)) {
                throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, adminProperties.lockMinutes());
            }
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        admin.recordLoginSuccess(now);
        log.info("Login success. username={}", admin.getUsername());

        return issueTokens(admin, now);
    }

    /**
     * 리프레시. 성공하면 새 액세스 토큰 + 새 리프레시 토큰을 돌려줍니다.
     */
    /**
     * ★ 여기도 noRollbackFor 가 필요합니다.
     *
     * 재사용이 탐지되면 revokeAllByAdminUserId 로 세션을 끊은 뒤 예외를 던지는데,
     * 롤백되면 그 폐기가 통째로 취소되어 탈취된 세션이 그대로 살아남습니다.
     * 즉 재사용 탐지가 "감지는 하지만 아무것도 막지 못하는" 코드가 됩니다.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResult refresh(String rawRefreshToken) {
        LocalDateTime now = LocalDateTime.now();
        String hash = sha256(rawRefreshToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

        // ★ 재사용 탐지 — 폐기된 토큰이 다시 왔다면 유출로 간주합니다.
        if (stored.isRevoked()) {
            int revoked = refreshTokenRepository.revokeAllByAdminUserId(stored.getAdminUserId(), now);
            log.warn("Refresh token reuse detected. adminUserId={} revokedSessions={}",
                    stored.getAdminUserId(), revoked);
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        if (stored.isExpired(now)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        AdminUser admin = adminUserRepository.findById(stored.getAdminUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

        if (!admin.isEnabled()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 회전 — 쓴 토큰은 폐기하고 새로 발급합니다.
        stored.revoke(now);

        return issueTokens(admin, now);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        // 로그아웃은 실패해도 사용자에게 알릴 게 없습니다. 조용히 폐기만 합니다.
        refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken))
                .ifPresent(token -> token.revoke(LocalDateTime.now()));
    }

    public AdminUser getById(Long adminUserId) {
        return adminUserRepository.findById(adminUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    // ===================== 내부 =====================

    private AuthResult issueTokens(AdminUser admin, LocalDateTime now) {
        String accessToken = tokenProvider.createAccessToken(admin);

        String rawRefreshToken = generateRefreshToken();
        refreshTokenRepository.save(new RefreshToken(
                admin.getId(),
                sha256(rawRefreshToken),
                now.plusDays(jwtProperties.refreshExpDays())
        ));

        TokenResponse tokens = new TokenResponse(
                accessToken,
                tokenProvider.accessTokenExpiresInSeconds(),
                admin.getUsername(),
                admin.getDisplayName(),
                admin.getRole()
        );

        return new AuthResult(tokens, rawRefreshToken);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * BCrypt 가 아니라 SHA-256 을 쓰는 이유:
     *   리프레시 토큰은 해시로 "조회"해야 하는데 BCrypt 는 매번 솔트가 달라 조회가 불가능합니다.
     *   또한 이 값은 사람이 만든 비밀번호가 아니라 256bit 난수라
     *   무차별 대입이 불가능해 느린 해시가 필요 없습니다.
     */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 사용할 수 없습니다.", e);
        }
    }

    /** 테스트에서 만료 토큰을 만들 때 씁니다. */
    Optional<RefreshToken> findByRawToken(String rawToken) {
        return refreshTokenRepository.findByTokenHash(sha256(rawToken));
    }
}