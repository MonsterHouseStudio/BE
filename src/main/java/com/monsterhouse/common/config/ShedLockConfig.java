package com.monsterhouse.common.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 여러 파드가 같은 @Scheduled 를 동시에 실행하는 것을 막습니다.
 *
 * 단일 인스턴스일 때는 없어도 되지만, replica 를 2개 이상 띄우는 순간
 * 개인정보 파기 배치가 매일 새벽 4시에 파드 수만큼 동시에 돌게 됩니다.
 * DELETE 라 결과 자체는 멱등이지만, 같은 행을 동시에 지우면서
 * 락 경합·데드락이 나고 로그도 중복으로 남습니다.
 *
 * 락은 별도 저장소 없이 DB 테이블(shedlock) 한 개로 잡습니다.
 * 이 정도 규모에 Redis 를 추가로 띄울 이유가 없습니다.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .withTableName("shedlock")

                        // ★ 락 만료 판정을 각 파드의 시계가 아니라 MySQL 시계로 합니다.
                        //   이게 없으면 파드 시계가 몇 초만 어긋나도
                        //   한쪽이 "락이 이미 풀렸다"고 판단해 이중 실행됩니다.
                        //   컨테이너는 호스트 시계를 따라가므로 노드가 다르면 실제로 벌어집니다.
                        .usingDbTime()

                        .build()
        );
    }
}