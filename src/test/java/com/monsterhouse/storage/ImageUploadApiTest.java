package com.monsterhouse.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monsterhouse.admin.entity.AdminRole;
import com.monsterhouse.admin.entity.AdminUser;
import com.monsterhouse.admin.repository.AdminUserRepository;
import com.monsterhouse.admin.repository.RefreshTokenRepository;
import com.monsterhouse.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 이미지 업로드 파이프라인과 검증 (기획서 §6.2, §9).
 *
 * curl 로 멀티파트를 보내면 셸/OS 마다 경로·인코딩 문제가 달라 재현이 어렵습니다.
 * MockMvc 로 고정해두면 CI 어디서 돌려도 같은 결과가 나옵니다.
 */
@AutoConfigureMockMvc
@DisplayName("이미지 업로드")
class ImageUploadApiTest extends IntegrationTestSupport {

    private static final String USERNAME = "uploader";
    private static final String PASSWORD = "upload1234!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        refreshTokenRepository.deleteAllInBatch();
        adminUserRepository.deleteAllInBatch();

        adminUserRepository.save(AdminUser.builder()
                .username(USERNAME)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .displayName("업로더")
                .role(AdminRole.SUPER_ADMIN)
                .build());

        String body = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"username":"%s","password":"%s"}
                                        """.formatted(USERNAME, PASSWORD)))
                .andReturn().getResponse().getContentAsString();

        accessToken = objectMapper.readTree(body).path("data").path("accessToken").asText();
    }

    /** 실제로 디코딩 가능한 PNG 를 만듭니다. */
    private byte[] realImage(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(139, 10, 10));
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }

    @Test
    @DisplayName("세로 이미지를 올리면 3종 키가 생성되고 ratio 가 portrait 로 판정된다")
    void uploadPortrait() throws Exception {
        mockMvc.perform(multipart("/api/admin/uploads/images")
                        .file(file("shot.png", "image/png", realImage(1200, 1600)))
                        .param("directory", "gallery")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.originalKey").isNotEmpty())
                .andExpect(jsonPath("$.data.thumbKey").isNotEmpty())
                .andExpect(jsonPath("$.data.width").value(1200))
                .andExpect(jsonPath("$.data.height").value(1600))
                .andExpect(jsonPath("$.data.ratio").value("portrait"));
    }

    @Test
    @DisplayName("가로/정사각 이미지의 ratio 판정")
    void ratioDetection() throws Exception {
        mockMvc.perform(multipart("/api/admin/uploads/images")
                        .file(file("wide.png", "image/png", realImage(1600, 900)))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(jsonPath("$.data.ratio").value("landscape"));

        mockMvc.perform(multipart("/api/admin/uploads/images")
                        .file(file("square.png", "image/png", realImage(800, 800)))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(jsonPath("$.data.ratio").value("square"));
    }

    @Test
    @DisplayName("원본이 medium 보다 작으면 같은 파일을 두 번 저장하지 않는다")
    void smallImageReusesOriginalAsMedium() throws Exception {
        mockMvc.perform(multipart("/api/admin/uploads/images")
                        .file(file("small.png", "image/png", realImage(1000, 1000)))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                // application-test 의 medium-width 는 기본 1600 → 1000px 원본은 medium 을 따로 만들지 않습니다.
                .andExpect(jsonPath("$.data.mediumKey").value(
                        org.hamcrest.Matchers.endsWith("_o.jpg")));
    }

    @Test
    @DisplayName("★ 이미지가 아닌 파일에 image/jpeg 를 붙여도 거부한다")
    void rejectsDisguisedFile() throws Exception {
        // Content-Type 헤더는 클라이언트가 지어낼 수 있으므로 실제 디코딩으로 판별해야 합니다.
        byte[] phpPayload = "<?php system($_GET[0]); ?>".repeat(50).getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(multipart("/api/admin/uploads/images")
                        .file(file("evil.jpg", "image/jpeg", phpPayload))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("F002"));
    }

    @Test
    @DisplayName("허용되지 않은 MIME 타입은 거부한다")
    void rejectsUnsupportedMime() throws Exception {
        mockMvc.perform(multipart("/api/admin/uploads/images")
                        .file(file("note.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("F002"));
    }

    @Test
    @DisplayName("화이트리스트에 없는 디렉터리는 거부한다")
    void rejectsUnknownDirectory() throws Exception {
        mockMvc.perform(multipart("/api/admin/uploads/images")
                        .file(file("shot.png", "image/png", realImage(400, 400)))
                        .param("directory", "../../etc")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("토큰 없이 업로드하면 401")
    void requiresAuth() throws Exception {
        mockMvc.perform(multipart("/api/admin/uploads/images")
                        .file(file("shot.png", "image/png", realImage(400, 400))))
                .andExpect(status().isUnauthorized());
    }
}
