-- docker-compose 최초 기동 시 1회 실행됩니다.
-- 스키마 자체는 JPA(ddl-auto) 또는 Flyway 가 만들고, 여기서는 DB 레벨 설정만 잡습니다.

ALTER DATABASE monsterhouse
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 동시성 테스트/디버깅용 권한 (락 상태 조회)
GRANT PROCESS ON *.* TO 'monster'@'%';
FLUSH PRIVILEGES;

-- ---------------------------------------------------------------------
-- ShedLock 락 테이블 — 여기가 유일한 예외입니다.
--
-- 위 주석대로 스키마는 JPA/Flyway 가 만드는 게 원칙이지만, 이 테이블만은
-- 로컬에서 아무도 만들어주지 않습니다:
--   · JPA 엔티티가 아니므로 ddl-auto: update 대상이 아니고
--   · local 프로파일은 flyway 가 꺼져 있어 V2__shedlock.sql 이 돌지 않습니다
--
-- 그대로 두면 새로 clone 한 개발자의 로컬에서 새벽 4시 파기 배치가
-- "Table 'monsterhouse.shedlock' doesn't exist" 로 실패합니다.
-- 운영(prod)에서는 Flyway V2 가 동일한 테이블을 만듭니다 — 정의를 맞춰두세요.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `shedlock` (
  `name`       VARCHAR(64)  NOT NULL,
  `lock_until` TIMESTAMP(3) NOT NULL,
  `locked_at`  TIMESTAMP(3) NOT NULL,
  `locked_by`  VARCHAR(255) NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
