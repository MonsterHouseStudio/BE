package com.monsterhouse.common.exception;

import com.monsterhouse.common.util.MessageUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLTransientConnectionException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 예외 → HTTP 상태 매핑 검증.
 *
 * ★ 왜 핸들러 메서드를 직접 호출하지 않고 MockMvc 를 쓰는가
 *   틀리기 쉬운 지점은 "메서드 안의 로직"이 아니라 "어떤 예외가 어떤 핸들러로 가는가"입니다.
 *   @ExceptionHandler 목록에서 한 줄 빠뜨리면 조용히 catch-all(500)로 떨어지는데,
 *   메서드를 직접 부르는 테스트는 그 라우팅을 전혀 검증하지 못합니다.
 *   실제로 이 프로젝트에서 그 방식으로 놓친 사례가 세 번 있었습니다
 *   (404→500, @PreAuthorize 403→500, 커넥션 풀 고갈 429→500).
 *
 *   DB 도 스프링 컨텍스트도 필요 없어 밀리초 단위로 끝납니다.
 */
@DisplayName("예외 → HTTP 상태 매핑")
class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler(messageUtil()))
            .build();

    private static MessageUtil messageUtil() {
        var source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:messages/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return new MessageUtil(source);
    }

    @Test
    @DisplayName("커넥션 풀 고갈은 500 이 아니라 429 로 안내한다")
    void poolExhaustionBecomes429() throws Exception {
        // 부하 실험에서 동시 50건 중 127건이 이 경로로 실패했습니다.
        // 서버가 고장난 게 아니라 잠깐 붐비는 것이므로 재시도를 유도해야 합니다.
        mockMvc.perform(get("/boom/pool"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C006"));
    }

    @Test
    @DisplayName("락 획득 실패도 429")
    void lockFailureBecomes429() throws Exception {
        mockMvc.perform(get("/boom/lock"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("C006"));
    }

    @Test
    @DisplayName("비즈니스 예외는 해당 상태코드로 나간다")
    void businessExceptionKeepsItsStatus() throws Exception {
        mockMvc.perform(get("/boom/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("B001"));
    }

    @Test
    @DisplayName("무결성 위반은 409")
    void dataIntegrityBecomes409() throws Exception {
        mockMvc.perform(get("/boom/integrity"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("권한 거부는 403 (catch-all 로 새지 않아야 함)")
    void accessDeniedBecomes403() throws Exception {
        mockMvc.perform(get("/boom/denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("C005"));
    }

    @Test
    @DisplayName("그 밖의 예외만 500 이다")
    void unexpectedBecomes500() throws Exception {
        mockMvc.perform(get("/boom/unknown"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("C001"));
    }

    /** 각 예외를 실제로 던지는 더미 컨트롤러 */
    @RestController
    static class ThrowingController {

        @GetMapping("/boom/pool")
        void pool() {
            // HikariCP 가 커넥션을 못 주면 SQLTransientConnectionException 을 던지고,
            // Spring 이 CannotGetJdbcConnectionException(= DataAccessResourceFailureException 하위)
            // 으로 감쌉니다. 실제 사슬과 같은 모양으로 만듭니다.
            throw new DataAccessResourceFailureException(
                    "Failed to obtain JDBC Connection",
                    new SQLTransientConnectionException(
                            "HikariPool-1 - Connection is not available, request timed out after 10000ms"));
        }

        @GetMapping("/boom/lock")
        void lock() {
            throw new CannotAcquireLockException("lock wait timeout");
        }

        @GetMapping("/boom/business")
        void business() {
            throw new BusinessException(ErrorCode.SLOT_ALREADY_TAKEN);
        }

        @GetMapping("/boom/integrity")
        void integrity() {
            throw new DataIntegrityViolationException("duplicate slot_key");
        }

        @GetMapping("/boom/denied")
        void denied() {
            throw new AccessDeniedException("Access Denied");
        }

        @GetMapping("/boom/unknown")
        void unknown() {
            throw new IllegalStateException("예상하지 못한 오류");
        }
    }
}
