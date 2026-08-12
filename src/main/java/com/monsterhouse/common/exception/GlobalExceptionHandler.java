package com.monsterhouse.common.exception;

import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.common.util.MessageUtil;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageUtil messageUtil;

    /** 비즈니스 규칙 위반 — 예상된 실패. 스택트레이스는 남기지 않습니다. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        log.info("BusinessException: {} args={}", code.getCode(), e.getArgs());

        return ResponseEntity
                .status(code.getStatus())
                .body(ApiResponse.fail(
                        code.getCode(),
                        messageUtil.get(code.getMessageKey(), e.getArgs())
                ));
    }

    /** @Valid 실패 — 필드별 메시지를 그대로 내려 프론트 폼에 매핑 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        List<ApiResponse.FieldError> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ApiResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ApiResponse.fail(
                        ErrorCode.INVALID_INPUT.getCode(),
                        messageUtil.get(ErrorCode.INVALID_INPUT.getMessageKey()),
                        fieldErrors
                ));
    }

    /**
     * HttpMessageNotReadableException 이 포함된 이유:
     *   본문 JSON 이 깨졌거나(잘못된 인코딩, 문법 오류) 타입이 맞지 않을 때 발생합니다.
     *   이건 클라이언트 잘못이므로 400 이어야 하는데, 빠뜨리면 catch-all 로 떨어져
     *   "서버 오류가 발생했습니다"(500) 가 나갑니다.
     *   프론트 개발자가 자기 요청 문제를 서버 장애로 오해하게 됩니다.
     */
    @ExceptionHandler({
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
        log.info("Bad request: {}", e.getMessage());
        return toResponse(ErrorCode.INVALID_INPUT);
    }

    /**
     * ★ 예약 동시성 3층 방어의 마지막 층.
     * UNIQUE(slot_key) 위반은 BookingService 가 잡아 SLOT_ALREADY_TAKEN 으로 바꿉니다.
     * 여기까지 온 건 그 밖의 제약 위반이므로 409 로 처리합니다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation", e);
        return toResponse(ErrorCode.DUPLICATE_RESOURCE);
    }

    /**
     * 날짜 락 대기 타임아웃/데드락, 그리고 커넥션 풀 고갈.
     * 실패가 아니라 "잠시 후 재시도" 상황이므로 429 로 안내합니다.
     *
     * ★ DataAccessResourceFailureException 이 포함된 이유 (부하 실험에서 발견)
     *   동시 50건을 넣었더니 실패 147건 중 127건이 커넥션 풀 고갈이었습니다.
     *   예약 스레드는 커넥션을 먼저 잡고 그 다음 날짜 락을 기다리는데,
     *   날짜 락이 요청을 직렬화하므로 대기하는 동안에도 커넥션을 계속 물고 있습니다.
     *   동시 요청이 풀 크기를 넘으면 나머지는 커넥션조차 못 받고 타임아웃됩니다.
     *
     *   이 예외를 빠뜨리면 catch-all 로 떨어져 "서버 오류(500)" 가 나갑니다.
     *   실제로는 서버가 고장난 게 아니라 잠깐 붐비는 것이므로 429 가 맞습니다.
     *   500 은 사용자가 재시도할 이유를 못 느끼게 만들고, 모니터링에서도
     *   진짜 장애와 구분되지 않습니다.
     *
     *   (HikariCP 의 SQLTransientConnectionException 은 Spring 이
     *    CannotGetJdbcConnectionException → DataAccessResourceFailureException 으로 감쌉니다)
     */
    @ExceptionHandler({
            CannotAcquireLockException.class,
            PessimisticLockingFailureException.class,
            DataAccessResourceFailureException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleOverload(Exception e) {
        log.warn("Overload or lock failure: {}", e.getClass().getSimpleName(), e);
        return toResponse(ErrorCode.TOO_MANY_REQUESTS);
    }

    /**
     * 존재하지 않는 경로.
     *
     * 이게 없으면 아래 catch-all 로 떨어져 오타난 URL 이 전부 500 으로 나갑니다.
     * 클라이언트 입장에서 "서버가 고장났다"와 "주소가 틀렸다"는 완전히 다른 문제입니다.
     * 스택트레이스도 남지 않도록 여기서 끊습니다.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception e) {
        log.debug("No handler for request: {}", e.getMessage());
        return toResponse(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException e) {
        log.debug("Method not supported: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail(
                        ErrorCode.NOT_FOUND.getCode(),
                        messageUtil.get(ErrorCode.NOT_FOUND.getMessageKey())));
    }

    /**
     * ★ @PreAuthorize 거부 (AuthorizationDeniedException extends AccessDeniedException)
     *
     * SecurityConfig 에 RestAccessDeniedHandler 를 달아뒀지만 그것만으로는 부족합니다.
     * 그 핸들러는 "필터 체인에서 막힌 요청"만 처리합니다.
     * @PreAuthorize 는 컨트롤러 메서드를 호출하는 시점에 예외를 던지므로
     * ExceptionTranslationFilter 까지 가지 않고 @RestControllerAdvice 가 먼저 잡습니다.
     *
     * 이 핸들러가 없으면 아래 catch-all 로 떨어져
     * "권한이 없습니다(403)" 여야 할 응답이 "서버 오류(500)" 로 나갑니다.
     * 실제로 MANAGER 가 SUPER_ADMIN 전용 API 를 호출했을 때 500 이 나왔습니다.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        log.info("Access denied: {}", e.getMessage());
        return toResponse(ErrorCode.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return toResponse(ErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<ApiResponse<Void>> toResponse(ErrorCode code) {
        HttpStatus status = code.getStatus();
        return ResponseEntity
                .status(status)
                .body(ApiResponse.fail(code.getCode(), messageUtil.get(code.getMessageKey())));
    }
}