package com.monsterhouse.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * ⚠ 동시성 테스트를 H2 로 하면 아무것도 검증하지 못합니다.
 *    SELECT ... FOR UPDATE 의 잠금 범위, INSERT IGNORE, 유니크 인덱스의 NULL 처리가
 *    MySQL 과 전부 다릅니다. 실제 MySQL 8 컨테이너로 검증합니다.
 *
 * 컨테이너는 static 이라 테스트 클래스 간에 재사용됩니다 (기동 비용 절약).
 */
@ActiveProfiles("test")
@SpringBootTest
public abstract class IntegrationTestSupport {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("monsterhouse_test")
            .withUsername("test")
            .withPassword("test")
            .withCommand(
                    "--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_unicode_ci",
                    "--default-time-zone=+09:00",
                    "--innodb-lock-wait-timeout=10"
            );

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}