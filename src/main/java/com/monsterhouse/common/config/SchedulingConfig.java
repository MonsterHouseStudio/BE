package com.monsterhouse.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Scheduled 활성화. 이게 없으면 파기 배치가 조용히 아무 일도 하지 않습니다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
