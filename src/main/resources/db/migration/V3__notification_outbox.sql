-- =====================================================================
--  MONSTER HOUSE — 알림 아웃박스 (V3)
--
--  왜 필요한가:
--    예전에는 LINE/메일 발송이 실패하면 log.error 만 남고 끝이었습니다.
--    예약은 DB 에 들어갔는데 사장님은 알림을 못 받는 상황이 조용히 발생합니다.
--    외부 API 의 일시 장애(5xx·타임아웃)는 드물지 않습니다.
--
--    이제 보내기 전에 여기에 적고, 실패하면 PENDING 으로 남겨
--    OutboxDispatcher 가 지수 백오프(1→2→4→8분, 5회 후 포기)로 재시도합니다.
--
--  ⚠ body 에 예약자 이름·연락처가 들어갑니다. 개인정보입니다.
--    발송 완료 후 30일이 지나면 파기 배치가 지웁니다 (기획서 §9).
--    GIVEN_UP 은 지우지 않습니다 — 왜 실패했는지 확인해야 할 대상입니다.
--
--  ⚠ 한 번 배포된 뒤에는 절대 수정하지 마세요 (Flyway 체크섬 검증).
-- =====================================================================

CREATE TABLE `notification_outbox` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `created_at`      DATETIME(6)  NOT NULL,
  `updated_at`      DATETIME(6)  NOT NULL,

  `channel`         ENUM('LINE','MAIL') COLLATE utf8mb4_unicode_ci NOT NULL,
  -- NULL 이면 각 sender 의 기본 수신자(사장님)로 갑니다.
  `recipient`       VARCHAR(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `subject`         VARCHAR(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `body`            TEXT         COLLATE utf8mb4_unicode_ci NOT NULL,

  `status`          ENUM('GIVEN_UP','PENDING','SENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `attempts`        INT          NOT NULL,
  -- 이 시각 이후에 재시도합니다. 지수 백오프로 늘려갑니다.
  `next_attempt_at` DATETIME(6)  NOT NULL,
  -- 마지막 실패 사유. 사람이 원인을 보려면 필요합니다.
  `last_error`      VARCHAR(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sent_at`         DATETIME(6)  DEFAULT NULL,

  PRIMARY KEY (`id`),

  -- 배치가 "보낼 것"만 골라내는 조건(status + next_attempt_at).
  -- 이 인덱스가 없으면 매 분 전체 스캔합니다.
  KEY `idx_outbox_status_next` (`status`, `next_attempt_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
