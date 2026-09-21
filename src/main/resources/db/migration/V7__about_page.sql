-- =====================================================================
--  MONSTER HOUSE — 소개 페이지 관리자 편집 (V7)
--
--  소개 페이지의 3개 섹션을 관리자에서 등록/수정/삭제할 수 있게 합니다.
--    1) about_intro : 메인 배너(제목·설명 한/일 + 콜라주 사진 3장) — 싱글턴(id=1)
--    2) crew        : 크루 구성원(이름·역할·소개 한/일 + 사진)
--    3) about_video : 최신 영상(유튜브 URL + 썸네일 + 제목 한/일)
--
--  ⚠ 한 번 배포된 뒤에는 절대 수정하지 마세요 (Flyway 체크섬 검증).
--  ⚠ INSERT 는 컬럼 수와 값 수가 정확히 일치해야 합니다 (V6 outage 재발 방지).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) 소개 인트로 (싱글턴)
-- ---------------------------------------------------------------------
CREATE TABLE `about_intro` (
  `id`         BIGINT       NOT NULL,
  `created_at` DATETIME(6)  NOT NULL,
  `updated_at` DATETIME(6)  NOT NULL,

  `title_ko`   VARCHAR(200)  COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title_ja`   VARCHAR(200)  COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `desc_ko`    VARCHAR(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `desc_ja`    VARCHAR(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,

  `photo1_key` VARCHAR(300)  COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `photo2_key` VARCHAR(300)  COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `photo3_key` VARCHAR(300)  COLLATE utf8mb4_unicode_ci DEFAULT NULL,

  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 싱글턴 1행 시드 (기존 소개 문구). 사진은 관리자가 나중에 등록합니다.
INSERT INTO `about_intro`
  (`id`,`created_at`,`updated_at`,`title_ko`,`title_ja`,`desc_ko`,`desc_ja`,`photo1_key`,`photo2_key`,`photo3_key`)
VALUES
  (1,NOW(6),NOW(6),
   '우리는 무대 뒤를 찍습니다',
   '私たちはステージの裏側を撮ります',
   'MONSTER HOUSE는 보디빌딩 선수와 센터를 위한 영상·사진 미디어입니다. 결과가 아니라 과정을, 포즈가 아니라 사람을 기록합니다.',
   'MONSTER HOUSE はボディビル選手とジムのための映像・写真メディアです。結果ではなく過程を、ポーズではなく人を記録します。',
   NULL,NULL,NULL);

-- ---------------------------------------------------------------------
-- 2) 크루
-- ---------------------------------------------------------------------
CREATE TABLE `crew` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `created_at` DATETIME(6)  NOT NULL,
  `updated_at` DATETIME(6)  NOT NULL,

  `name_ko`    VARCHAR(60)  COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name_ja`    VARCHAR(60)  COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role_ko`    VARCHAR(60)  COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role_ja`    VARCHAR(60)  COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bio_ko`     VARCHAR(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bio_ja`     VARCHAR(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,

  `photo_key`  VARCHAR(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,

  `active`     BIT(1)       NOT NULL,
  `sort_order` INT          NOT NULL,

  PRIMARY KEY (`id`),
  KEY `idx_crew_active` (`active`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 기본 3명 시드. 사진은 관리자가 나중에 등록합니다.
INSERT INTO `crew`
  (`created_at`,`updated_at`,`name_ko`,`name_ja`,`role_ko`,`role_ja`,`bio_ko`,`bio_ja`,`photo_key`,`active`,`sort_order`)
VALUES
  (NOW(6),NOW(6),'정재윤','チョン・ジェユン','디렉터 · 촬영','ディレクター・撮影','기록하는 사람. 무대보다 무대 뒤를 오래 본다.','記録する人。ステージよりも舞台裏を長く見つめる。',NULL,b'1',0),
  (NOW(6),NOW(6),'크루 A','クルー A','편집 · 컬러','編集・カラー','숫자보다 톤을 먼저 맞춘다.','数値よりトーンを先に合わせる。',NULL,b'1',1),
  (NOW(6),NOW(6),'크루 B','クルー B','통역 · 코디네이션','通訳・コーディネート','한국과 일본 사이를 오간다.','韓国と日本の間を行き来する。',NULL,b'1',2);

-- ---------------------------------------------------------------------
-- 3) 최신 영상 (유튜브). 실제 URL 이 필요하므로 시드는 넣지 않습니다.
-- ---------------------------------------------------------------------
CREATE TABLE `about_video` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `created_at`    DATETIME(6)  NOT NULL,
  `updated_at`    DATETIME(6)  NOT NULL,

  `youtube_url`   VARCHAR(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title_ko`      VARCHAR(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title_ja`      VARCHAR(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `thumbnail_key` VARCHAR(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,

  `active`        BIT(1)       NOT NULL,
  `sort_order`    INT          NOT NULL,

  PRIMARY KEY (`id`),
  KEY `idx_about_video_active` (`active`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
