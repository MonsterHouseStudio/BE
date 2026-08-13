-- =====================================================================
--  MONSTER HOUSE — 홈 히어로 배너 (V4)
--
--  이미지 배너와 MP4 배경 영상 배너를 함께 다룹니다.
--
--  ⚠ URL 이 아니라 key 를 저장합니다.
--    URL 을 박아두면 CDN 도메인이 바뀌는 순간 과거 데이터가 전부 깨집니다.
--    (gallery_item·post 와 같은 규칙)
--
--  ⚠ poster_key 는 VIDEO 일 때 사실상 필수입니다.
--    iOS 는 muted + playsinline 이어야 자동재생되고, 저전력/데이터절약
--    모드에서는 아예 재생하지 않습니다. 그때 포스터가 없으면 히어로가
--    검은 화면이 됩니다. NULL 허용은 IMAGE 배너 때문이고,
--    VIDEO 인데 비어 있으면 서비스 계층이 저장을 거부합니다.
--
--  ⚠ 한 번 배포된 뒤에는 절대 수정하지 마세요 (Flyway 체크섬 검증).
-- =====================================================================

CREATE TABLE `banner` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `created_at`  DATETIME(6)  NOT NULL,
  `updated_at`  DATETIME(6)  NOT NULL,

  `media_type`  ENUM('IMAGE','VIDEO') COLLATE utf8mb4_unicode_ci NOT NULL,
  -- IMAGE 면 사진, VIDEO 면 mp4 의 저장 key
  `media_key`   VARCHAR(300) COLLATE utf8mb4_unicode_ci NOT NULL,
  -- VIDEO 일 때 첫 프레임·자동재생 실패 시 보여줄 이미지
  `poster_key`  VARCHAR(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,

  -- 문구. 비어 있으면 프론트의 기존 i18n 기본값을 씁니다.
  -- 일본어가 비면 한국어로 폴백합니다 (기획서 §3.2 — 배너는 첫 화면이라
  -- 비어 보이는 것이 최악이므로 상품과 같은 폴백 정책을 씁니다).
  `headline_ko` VARCHAR(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `headline_ja` VARCHAR(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `subtext_ko`  VARCHAR(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `subtext_ja`  VARCHAR(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,

  `active`      BIT(1)       NOT NULL,
  `sort_order`  INT          NOT NULL,

  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
