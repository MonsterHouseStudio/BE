package com.monsterhouse.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Flyway 마이그레이션 정합성")
@ActiveProfiles("test")
@SpringBootTest(properties = {
        // 운영과 같은 조건으로 맞춥니다.
        "spring.flyway.enabled=true",
        // 빈 DB 에서 V1 부터 전부 적용되어야 합니다. baseline 을 켜면 건너뛸 수 있습니다.
        "spring.flyway.baseline-on-migrate=false",
        // ★ 이게 이 테스트의 핵심입니다. 스키마와 엔티티가 다르면 여기서 죽습니다.
        "spring.jpa.hibernate.ddl-auto=validate",
        // 배치는 이 테스트와 무관합니다. shedlock 은 V2 가 만들지만 굳이 돌릴 이유가 없습니다.
        "app.retention.enabled=false",
        "app.notification.retry.enabled=false"
})
class FlywayMigrationTest {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("monsterhouse_migration")
            .withUsername("test")
            .withPassword("test")
            .withCommand(
                    "--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_unicode_ci",
                    "--default-time-zone=+09:00"
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("모든 마이그레이션이 성공적으로 적용된다")
    void allMigrationsApplied() {
        List<String> versions = jdbcTemplate.queryForList("""
                select version from flyway_schema_history
                where success = 1 and type = 'SQL'
                order by installed_rank
                """, String.class);

        assertThat(versions).containsExactly("1", "2", "3");
    }

    @Test
    @DisplayName("마이그레이션 파일 수와 적용된 수가 일치한다")
    void everyMigrationFileIsApplied() throws Exception {
        Resource[] files = new PathMatchingResourcePatternResolver()
                .getResources("classpath:db/migration/V*__*.sql");

        Integer applied = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = 1 and type = 'SQL'",
                Integer.class);

        assertThat(applied)
                .as("db/migration 의 V*__*.sql 파일이 전부 적용되어야 합니다. "
                        + "밑줄이 하나(V4_foo.sql)면 Flyway 가 조용히 무시합니다.")
                .isEqualTo(files.length);
    }

    /**
     * JPA 엔티티가 아닌 테이블은 ddl-auto=validate 가 검사하지 않습니다.
     * 없어도 기동은 되지만 런타임에 터지므로 여기서 따로 확인합니다.
     */
    @Test
    @DisplayName("엔티티가 아닌 테이블도 마이그레이션이 만든다")
    void nonEntityTablesExist() {
        // shedlock — 없으면 @Scheduled 가 매번 실패합니다 (ShedLock 이 여기에 락을 씁니다)
        assertThat(tableExists("shedlock"))
                .as("V2 가 만들어야 합니다. JPA 엔티티가 아니라 ddl-auto 로는 생기지 않습니다.")
                .isTrue();
    }

    @Test
    @DisplayName("핵심 테이블과 제약이 살아 있다")
    void coreConstraintsExist() {
        // 예약 동시성 3층 방어의 마지막 층. 이게 없으면 중복 예약을 DB 가 못 막습니다.
        Integer uniqueSlotKey = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.statistics
                where table_schema = database()
                  and table_name = 'booking'
                  and index_name = 'uk_booking_slot_key'
                  and non_unique = 0
                """, Integer.class);
        assertThat(uniqueSlotKey)
                .as("UNIQUE(slot_key) 는 동시성 방어의 최후 보루입니다. 절대 제거하면 안 됩니다.")
                .isGreaterThan(0);

        // 아웃박스 조회 인덱스 — 없으면 배치가 매 분 전체 스캔합니다.
        Integer outboxIndex = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.statistics
                where table_schema = database()
                  and table_name = 'notification_outbox'
                  and index_name = 'idx_outbox_status_next'
                """, Integer.class);
        assertThat(outboxIndex).isGreaterThan(0);
    }

    @Test
    @DisplayName("문자셋이 utf8mb4 다 (한글·일본어·이모지)")
    void charsetIsUtf8mb4() {
        // utf8(3바이트)이면 이모지에서 깨집니다. 일본어 고객 문의에 실제로 들어옵니다.
        String collation = jdbcTemplate.queryForObject(
                "select @@collation_database", String.class);
        assertThat(collation).startsWith("utf8mb4");
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = database() and table_name = ?
                """, Integer.class, table);
        return count != null && count > 0;
    }
}