-- =====================================================================
--  MONSTER HOUSE — 홈 "숫자로 보는" 풀스크린 스크롤 패널 (V6)
--
--  큰 값(숫자 카운트업 또는 텍스트) + 라벨 + 설명문 + 배경 사진.
--  숫자는 관리자에서 편집합니다.
--
--  ⚠ 한 번 배포된 뒤에는 절대 수정하지 마세요 (Flyway 체크섬 검증).
-- =====================================================================

CREATE TABLE `home_stat` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `created_at`   DATETIME(6)  NOT NULL,
  `updated_at`   DATETIME(6)  NOT NULL,

  -- 카운트업 목표 숫자(없으면 value_text 를 그대로 표시)
  `value_number` INT          DEFAULT NULL,
  `suffix`       VARCHAR(10)  COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `value_text`   VARCHAR(40)  COLLATE utf8mb4_unicode_ci DEFAULT NULL,

  `label_ko`     VARCHAR(60)  COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `label_ja`     VARCHAR(60)  COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `desc_ko`      VARCHAR(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `desc_ja`      VARCHAR(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,

  `photo_key`    VARCHAR(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,

  `active`       BIT(1)       NOT NULL,
  `sort_order`   INT          NOT NULL,

  PRIMARY KEY (`id`),
  KEY `idx_home_stat_active` (`active`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 기본 4개 시드 (기존 홈 통계 값). 사진은 관리자가 나중에 등록합니다.
INSERT INTO `home_stat`
  (`created_at`,`updated_at`,`value_number`,`suffix`,`value_text`,`label_ko`,`label_ja`,`desc_ko`,`desc_ja`,`photo_key`,`active`,`sort_order`)
VALUES
  (NOW(6),NOW(6),480,'+',NULL,'누적 촬영','累計撮影','무대 뒤부터 결과물까지, 그동안 쌓아 올린 촬영의 기록입니다.','舞台裏から仕上がりまで、積み重ねてきた撮影の記録です。',NULL,b'1',0),
  (NOW(6),NOW(6),120,'+',NULL,'함께한 선수','共に歩んだ選手','한 무대를 위해 함께 준비한 선수들의 숫자입니다.','一つの舞台のために共に準備した選手の数です。',NULL,b'1',1),
  (NOW(6),NOW(6),4,'',NULL,'운영 연차','運営年数','현장에서 쌓아 온 시간이 곧 이해의 깊이가 됩니다.','現場で積み重ねた時間が、そのまま理解の深さになります。',NULL,b'1',2),
  (NOW(6),NOW(6),NULL,'','KR · JP','한국·일본','한국과 일본, 두 무대를 잇는 촬영과 통역을 합니다.','韓国と日本、二つの舞台をつなぐ撮影と通訳を行います。',NULL,b'1',3);
