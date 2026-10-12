-- =====================================================================
--  MONSTER HOUSE — 고객 회원 (이메일 + 비밀번호) (V10)
--
--  예약은 이메일로 연결하므로 booking 스키마 변경은 없습니다.
--
--  ⚠ 한 번 배포된 뒤에는 절대 수정하지 마세요 (Flyway 체크섬 검증).
-- =====================================================================

CREATE TABLE `member` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `created_at`    DATETIME(6)  NOT NULL,
  `updated_at`    DATETIME(6)  NOT NULL,

  `email`         VARCHAR(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` VARCHAR(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name`          VARCHAR(50)  COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone`         VARCHAR(30)  COLLATE utf8mb4_unicode_ci NOT NULL,

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
