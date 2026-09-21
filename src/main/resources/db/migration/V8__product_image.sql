-- =====================================================================
--  MONSTER HOUSE — 촬영 상품 대표 이미지 (V8)
--
--  상품 등록/수정 시 대표 이미지를 올릴 수 있게 image_key 컬럼을 추가합니다.
--  기존 상품은 NULL(이미지 없음)로 남고, 관리자가 이후 업로드합니다.
--
--  ⚠ 한 번 배포된 뒤에는 절대 수정하지 마세요 (Flyway 체크섬 검증).
-- =====================================================================

ALTER TABLE `product`
  ADD COLUMN `image_key` VARCHAR(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `sort_order`;
