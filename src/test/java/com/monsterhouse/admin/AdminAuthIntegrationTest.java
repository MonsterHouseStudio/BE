package com.monsterhouse.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monsterhouse.admin.dto.LoginRequest;
import com.monsterhouse.admin.entity.AdminRole;
import com.monsterhouse.admin.entity.AdminUser;
import com.monsterhouse.admin.repository.AdminUserRepository;
import com.monsterhouse.admin.repository.RefreshTokenRepository;
import com.monsterhouse.support.IntegrationTestSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DisplayName("관리자 인증")
class AdminAuthIntegrationTest extends IntegrationTestSupport {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin1234!";
    private static final String REFRESH_COOKIE = "mh_refresh";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAllInBatch();
        adminUserRepository.deleteAllInBatch();

        adminUserRepository.save(AdminUser.builder()
                .username(USERNAME)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .displayName("운영자")
                .role(AdminRole.SUPER_ADMIN)
                .build());
    }

    @Test
    @DisplayName("로그인하면 액세스 토큰은 본문으로, 리프레시 토큰은 httpOnly 쿠키로 나간다")
    void loginIssuesTokens() throws Exception {
        MvcResult result = login(USERNAME, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.role").value("SUPER_ADMIN"))
                // 리프레시 토큰이 본문에 새어 나오면 httpOnly 를 쓴 의미가 없습니다.
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getValue()).isNotBlank();
    }

    @Test
    @DisplayName("비밀번호가 틀리면 401, 존재하지 않는 아이디와 같은 응답을 준다")
    void wrongPasswordAndUnknownUserLookIdentical() throws Exception {
        String bodyForWrongPassword = login(USERNAME, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String bodyForUnknownUser = login("no-such-user", "whatever")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        // 응답이 다르면 아이디 존재 여부를 스캔당합니다.
        assertThat(bodyForWrongPassword).isEqualTo(bodyForUnknownUser);
    }

    @Test
    @DisplayName("5회 실패하면 계정이 잠기고, 올바른 비밀번호로도 들어갈 수 없다")
    void lockAfterMaxAttempts() throws Exception {
        for (int i = 0; i < 4; i++) {
            login(USERNAME, "wrong-password").andExpect(status().isUnauthorized());
        }

        // 5번째 실패에서 잠김 (423 Locked)
        login(USERNAME, "wrong-password").andExpect(status().isLocked());

        // 비밀번호가 맞아도 잠금이 우선입니다.
        login(USERNAME, PASSWORD).andExpect(status().isLocked());
    }

    @Test
    @DisplayName("액세스 토큰 없이 보호된 API 를 호출하면 401")
    void protectedApiRequiresToken() throws Exception {
        mockMvc.perform(get("/api/admin/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("C004"));
    }

    @Test
    @DisplayName("발급받은 액세스 토큰으로 보호된 API 를 호출할 수 있다")
    void protectedApiWithToken() throws Exception {
        String accessToken = extractAccessToken(login(USERNAME, PASSWORD).andReturn());

        mockMvc.perform(get("/api/admin/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(USERNAME));
    }

    @Test
    @DisplayName("리프레시하면 새 토큰이 발급되고 쓴 리프레시 토큰은 폐기된다")
    void refreshRotatesToken() throws Exception {
        Cookie oldCookie = login(USERNAME, PASSWORD).andReturn().getResponse().getCookie(REFRESH_COOKIE);

        MvcResult refreshed = mockMvc.perform(post("/api/admin/auth/refresh").cookie(oldCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        Cookie newCookie = refreshed.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(newCookie).isNotNull();
        assertThat(newCookie.getValue()).isNotEqualTo(oldCookie.getValue());
    }

    @Test
    @DisplayName("★ 폐기된 리프레시 토큰이 재사용되면 그 계정의 모든 세션을 끊는다")
    void refreshTokenReuseRevokesAllSessions() throws Exception {
        Cookie stolenCookie = login(USERNAME, PASSWORD).andReturn().getResponse().getCookie(REFRESH_COOKIE);

        // 정상 사용자가 한 번 리프레시 → stolenCookie 는 폐기됨
        Cookie currentCookie = mockMvc.perform(post("/api/admin/auth/refresh").cookie(stolenCookie))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(REFRESH_COOKIE);

        // 공격자가 훔쳐둔 옛 토큰으로 시도 → 거부
        mockMvc.perform(post("/api/admin/auth/refresh").cookie(stolenCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("A005"));

        // 그리고 정상 사용자의 현재 토큰까지 함께 끊깁니다.
        // 누가 진짜인지 알 수 없으므로 전부 끊고 재로그인시키는 것이 안전합니다.
        mockMvc.perform(post("/api/admin/auth/refresh").cookie(currentCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃하면 리프레시 토큰이 무효화되고 쿠키가 지워진다")
    void logout() throws Exception {
        Cookie cookie = login(USERNAME, PASSWORD).andReturn().getResponse().getCookie(REFRESH_COOKIE);

        MvcResult result = mockMvc.perform(post("/api/admin/auth/logout").cookie(cookie))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getCookie(REFRESH_COOKIE).getMaxAge()).isZero();

        mockMvc.perform(post("/api/admin/auth/refresh").cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    // ===================== 헬퍼 =====================

    private org.springframework.test.web.servlet.ResultActions login(String username, String password)
            throws Exception {
        return mockMvc.perform(post("/api/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password))));
    }

    private String extractAccessToken(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }
}