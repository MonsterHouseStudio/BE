package com.monsterhouse.inquiry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monsterhouse.common.ratelimit.RateLimiter;
import com.monsterhouse.inquiry.dto.request.InquiryCreateRequest;
import com.monsterhouse.inquiry.entity.Inquiry;
import com.monsterhouse.inquiry.entity.InquiryStatus;
import com.monsterhouse.inquiry.entity.InquiryType;
import com.monsterhouse.inquiry.repository.InquiryRepository;
import com.monsterhouse.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("문의 접수")
class InquiryApiTest extends IntegrationTestSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private InquiryRepository inquiryRepository;
    @Autowired private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        inquiryRepository.deleteAllInBatch();
        // 고정 윈도가 테스트 간에 넘어오면 뒤 테스트가 429 로 죽습니다.
        rateLimiter.reset();
    }

    private InquiryCreateRequest request(String honeypot) {
        return new InquiryCreateRequest(
                InquiryType.INTERPRETER,
                "田中 健",
                "LINE: tanaka_k",
                "tanaka@example.jp",
                "10月の大阪オープンに参加します。計量と申込の通訳をお願いしたいです。",
                true,
                honeypot);
    }

    private ResultActions submit(InquiryCreateRequest body, String locale) throws Exception {
        return mockMvc.perform(post("/api/inquiries")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Locale", locale)
                .content(objectMapper.writeValueAsString(body)));
    }

    @Test
    @DisplayName("신청이 저장되고 신청 당시 언어가 함께 기록된다")
    void createStoresLocale() throws Exception {
        submit(request(null), "ja")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber());

        List<Inquiry> saved = inquiryRepository.findAll();
        assertThat(saved).hasSize(1);

        Inquiry inquiry = saved.get(0);
        assertThat(inquiry.getType()).isEqualTo(InquiryType.INTERPRETER);
        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.PENDING);
        // 회신을 어느 언어로 쓸지 판단하는 근거이므로 반드시 남아야 합니다.
        assertThat(inquiry.getLocale().getCode()).isEqualTo("ja");
    }

    @Test
    @DisplayName("honeypot 에 값이 차 있으면 거부하고 저장하지 않는다")
    void honeypotRejected() throws Exception {
        submit(request("http://spam.example"), "ko")
                .andExpect(status().isBadRequest());

        assertThat(inquiryRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("개인정보 동의를 하지 않으면 400 이고 저장되지 않는다")
    void privacyRequired() throws Exception {
        var body = new InquiryCreateRequest(
                InquiryType.VIDEO, "김성호", "010-2345-6789", "sungho@example.com",
                "센터 홍보 영상 문의드립니다.", false, null);

        submit(body, "ko").andExpect(status().isBadRequest());

        assertThat(inquiryRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("같은 IP 가 한도를 넘겨 제출하면 429 (application-test 의 capacity=3)")
    void rateLimited() throws Exception {
        for (int i = 0; i < 3; i++) {
            submit(request(null), "ko").andExpect(status().isCreated());
        }

        submit(request(null), "ko")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("C006"));

        // 한도를 넘긴 요청은 저장되지 않아야 합니다.
        assertThat(inquiryRepository.findAll()).hasSize(3);
    }

    @Test
    @DisplayName("조회(GET)는 횟수 제한 대상이 아니다")
    void getNotRateLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/competitions")).andExpect(status().isOk());
        }
    }
}
