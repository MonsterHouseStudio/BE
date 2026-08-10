-- =====================================================================
--  MONSTER HOUSE — ShedLock 락 테이블 (V2)
--
--  왜 필요한가:
--    파드/인스턴스가 2대 이상이면 @Scheduled 가 대수만큼 동시에 실행됩니다.
--    개인정보 파기 배치(매일 04:00)가 중복으로 돌면서 같은 행을 동시에 지워
--    락 경합·데드락이 납니다. 이 테이블 한 개로 "한 대만 실행"을 보장합니다.
--
--  ⚠ 이 테이블은 JPA 엔티티가 아니므로 ddl-auto 로는 절대 생성되지 않습니다.
--    Flyway 가 꺼진 프로파일(local, test)에서는 수동 생성이 필요합니다.
--
--  ⚠ 한 번 배포된 뒤에는 절대 수정하지 마세요 (Flyway 체크섬 검증).
-- =====================================================================

CREATE TABLE `shedlock` (
  -- @SchedulerLock(name = "...") 값이 그대로 들어갑니다.
  `name`       VARCHAR(64)  NOT NULL,

  -- 이 시각까지 락이 유지됩니다. lockAtMostFor 로 계산됩니다.
  -- 배치 도중 파드가 죽어도 이 시각이 지나면 자동으로 풀립니다.
  `lock_until` TIMESTAMP(3) NOT NULL,

  -- usingDbTime() 을 쓰므로 이 값들은 MySQL 시계로 기록됩니다.
  -- 파드마다 시계가 달라도 만료 판정이 흔들리지 않습니다.
  `locked_at`  TIMESTAMP(3) NOT NULL,

  -- 어느 인스턴스가 잡았는지. 장애 조사용입니다.
  `locked_by`  VARCHAR(255) NOT NULL,

  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
