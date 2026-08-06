package com.monsterhouse.admin;

import com.monsterhouse.admin.entity.AdminRole;
import com.monsterhouse.admin.entity.AdminUser;
import com.monsterhouse.admin.security.AdminPrincipal;
import com.monsterhouse.admin.security.JwtTokenProvider;
import com.monsterhouse.common.config.JwtProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 스프링 컨텍스트 없이 도는 순수 단위 테스트입니다. */
@DisplayName("JWT 액세스 토큰")
class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-secret-key-at-least-32-bytes-long";

    private JwtProperties props(long accessExpMinutes) {
        return new JwtProperties(SECRET, accessExpMinutes, 14, "monsterhouse-test",
                "mh_refresh", false, "Lax");
    }

    private AdminUser admin() {
        AdminUser admin = AdminUser.builder()
                .username("admin")
                .passwordHash("x")
                .displayName("운영자")
                .role(AdminRole.SUPER_ADMIN)
                .build();
        ReflectionTestUtils.setField(admin, "id", 1L);
        return admin;
    }

    @Test
    @DisplayName("발급한 토큰을 다시 읽으면 같은 관리자 정보가 나온다")
    void issueAndParse() {
        JwtTokenProvider provider = new JwtTokenProvider(props(30));

        String token = provider.createAccessToken(admin());
        AdminPrincipal principal = provider.parseAccessToken(token);

        assertThat(principal.id()).isEqualTo(1L);
        assertThat(principal.username()).isEqualTo("admin");
        assertThat(principal.role()).isEqualTo(AdminRole.SUPER_ADMIN);
    }

    @Test
    @DisplayName("만료된 토큰은 TokenExpiredException")
    void expired() {
        // 유효기간 0분 → 발급 즉시 만료
        JwtTokenProvider provider = new JwtTokenProvider(props(0));
        String token = provider.createAccessToken(admin());

        assertThatThrownBy(() -> provider.parseAccessToken(token))
                .isInstanceOf(JwtTokenProvider.TokenExpiredException.class);
    }

    @Test
    @DisplayName("서명이 다른 토큰은 InvalidTokenException")
    void tamperedSignature() {
        String token = new JwtTokenProvider(props(30)).createAccessToken(admin());

        JwtProperties otherSecret = new JwtProperties(
                "completely-different-secret-key-32-bytes-x", 30, 14,
                "monsterhouse-test", "mh_refresh", false, "Lax");

        assertThatThrownBy(() -> new JwtTokenProvider(otherSecret).parseAccessToken(token))
                .isInstanceOf(JwtTokenProvider.InvalidTokenException.class);
    }

    @Test
    @DisplayName("발급자가 다른 토큰은 거부한다")
    void wrongIssuer() {
        String token = new JwtTokenProvider(props(30)).createAccessToken(admin());

        JwtProperties otherIssuer = new JwtProperties(SECRET, 30, 14, "someone-else",
                "mh_refresh", false, "Lax");

        assertThatThrownBy(() -> new JwtTokenProvider(otherIssuer).parseAccessToken(token))
                .isInstanceOf(JwtTokenProvider.InvalidTokenException.class);
    }

    @Test
    @DisplayName("시크릿이 32바이트 미만이면 기동 시점에 실패한다")
    void shortSecretRejected() {
        JwtProperties tooShort = new JwtProperties("short", 30, 14, "monsterhouse-test",
                "mh_refresh", false, "Lax");

        assertThatThrownBy(() -> new JwtTokenProvider(tooShort))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32바이트");
    }
}