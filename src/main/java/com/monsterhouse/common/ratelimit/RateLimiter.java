package com.monsterhouse.common.ratelimit;

import com.monsterhouse.common.config.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class RateLimiter {
    private final RateLimitProperties properties;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private record Window(Instant startedAt, AtomicInteger count){}
    public boolean tryConsume(String key){
        if(!properties.enabled()){
            return true;
        }
        Instant now = Instant.now();
        Duration window = Duration.ofSeconds(properties.windowSeconds());
        Window current = windows.compute(key, (k, existing) -> {
            if (existing == null || Duration.between(existing.startedAt(), now).compareTo(window) >= 0) {
                return new Window(now, new AtomicInteger(0));
            }
            return existing;
        });
        return current.count().incrementAndGet() <= properties.capacity();
    }
    public void evictExpired(){
        Instant now = Instant.now();
        Duration window = Duration.ofSeconds(properties.windowSeconds());
        windows.entrySet().removeIf(e -> Duration.between(e.getValue().startedAt(), now).compareTo(window) >= 0);
    }
    public void reset(){
        windows.clear();
    }
}
