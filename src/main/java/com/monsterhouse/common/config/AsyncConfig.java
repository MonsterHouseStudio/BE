package com.monsterhouse.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 알림 발송 전용 풀.
 * 기획서 §5.3 — LINE/메일 발송 실패가 예약·문의 트랜잭션을 실패시키면 안 되므로
 * 커밋 이후 별도 스레드에서 처리합니다.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    public static final String NOTIFICATION_EXECUTOR = "notificationExecutor";

    @Bean(name = NOTIFICATION_EXECUTOR)
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("notify-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        // 큐가 꽉 차면 버리지 않고 호출 스레드에서 실행 (알림 유실 방지)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return notificationExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}