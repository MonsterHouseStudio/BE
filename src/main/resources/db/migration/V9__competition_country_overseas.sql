-- =====================================================================
--  MONSTER HOUSE — 시합 일정 국가에 '해외(OVERSEAS)' 추가 (V9)
--
--  기존 country ENUM('JP','KR') → ENUM('JP','KR','OVERSEAS').
--  글로벌 대회 일정(미국·영국·브라질 등)을 분류하기 위함.
--  기존 값(JP/KR)은 그대로 유지됩니다.
--
--  ⚠ 한 번 배포된 뒤에는 절대 수정하지 마세요 (Flyway 체크섬 검증).
-- =====================================================================

ALTER TABLE `competition`
  MODIFY COLUMN `country` ENUM('JP','KR','OVERSEAS')
  COLLATE utf8mb4_unicode_ci NOT NULL;
