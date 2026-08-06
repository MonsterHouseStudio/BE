package com.monsterhouse.common.ratelimit;

import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    /** 매 요청마다 정리하면 낭비라 주기적으로만 청소합니다. */
    private static final int EVICT_EVERY = 500;

    private final RateLimiter rateLimiter;
    private final AtomicInteger requestCount = new AtomicInteger();

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {

        // 조회는 막지 않습니다. 쓰기(폼 제출)만 대상입니다.
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }

        if (requestCount.incrementAndGet() % EVICT_EVERY == 0) {
            rateLimiter.evictExpired();
        }

        String key = request.getRequestURI() + "|" + clientIp(request);

        if (!rateLimiter.tryConsume(key)) {
            log.info("Rate limit exceeded. key={}", key);
            // GlobalExceptionHandler 가 429 + 다국어 메시지로 변환합니다.
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }

        return true;
    }

    /**
     * nginx 뒤에서는 getRemoteAddr() 가 프록시 IP 가 됩니다.
     * server.forward-headers-strategy: framework 를 켜뒀으므로
     * Spring 이 X-Forwarded-For 를 반영한 값을 돌려줍니다.
     */
    private String clientIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return ip == null ? "unknown" : ip;
    }
}