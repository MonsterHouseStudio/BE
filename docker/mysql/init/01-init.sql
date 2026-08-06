-- docker-compose 최초 기동 시 1회 실행됩니다.
-- 스키마 자체는 JPA(ddl-auto) 또는 Flyway 가 만들고, 여기서는 DB 레벨 설정만 잡습니다.

ALTER DATABASE monsterhouse
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 동시성 테스트/디버깅용 권한 (락 상태 조회)
GRANT PROCESS ON *.* TO 'monster'@'%';
FLUSH PRIVILEGES;
