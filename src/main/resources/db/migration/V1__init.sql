-- =====================================================================
--  MONSTER HOUSE — 초기 스키마 (V1)
--
--  생성 방법:
--    docker exec monsterhouse-mysql mysqldump -umonster -p<pw> --      --no-data --skip-comments --skip-add-drop-table --set-gtid-purged=OFF --      monsterhouse
--    그 뒤 AUTO_INCREMENT=NN 제거 + 아래 주석 정리.
--
--  ⚠ 이 파일은 한 번 배포된 뒤에는 절대 수정하지 마세요.
--    Flyway 가 체크섬으로 검증하므로 내용이 바뀌면 기동이 실패합니다.
--    스키마 변경은 반드시 새 파일(V2__*.sql, V3__*.sql)로 추가합니다.
-- =====================================================================

-- booking 은 product 를 참조하는데 알파벳 순서상 product 보다 먼저 생성됩니다.
-- 테이블 생성 순서를 손으로 맞추는 대신 검사를 잠시 꺼둡니다.
-- (파일 끝에서 반드시 되돌립니다)
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `admin_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `display_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `enabled` bit(1) NOT NULL,
  `failed_login_count` int NOT NULL,
  `last_login_at` datetime(6) DEFAULT NULL,
  `locked_until` datetime(6) DEFAULT NULL,
  `password_hash` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` enum('MANAGER','SUPER_ADMIN') COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `availability` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `active` bit(1) NOT NULL,
  `close_time` time(6) NOT NULL,
  `day_of_week` enum('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') COLLATE utf8mb4_unicode_ci NOT NULL,
  `open_time` time(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_availability_dow` (`day_of_week`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `availability_exception` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `close_time` time(6) DEFAULT NULL,
  `memo` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `open_time` time(6) DEFAULT NULL,
  `override_date` date NOT NULL,
  `type` enum('HOLIDAY','SPECIAL') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_availability_exception_date` (`override_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `booking` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `base_price` decimal(12,0) NOT NULL,
  `booking_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cancel_reason` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `canceled_at` datetime(6) DEFAULT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `email` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `end_at` datetime(6) NOT NULL,
  `locale` enum('JA','KO') COLLATE utf8mb4_unicode_ci NOT NULL,
  `memo` text COLLATE utf8mb4_unicode_ci,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `privacy_agreed` bit(1) NOT NULL,
  `slot_key` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `start_at` datetime(6) NOT NULL,
  `status` enum('CANCELED','COMPLETED','CONFIRMED','REQUESTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_price` decimal(12,0) NOT NULL,
  `product_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_booking_code` (`booking_code`),
  UNIQUE KEY `uk_booking_slot_key` (`slot_key`),
  KEY `idx_booking_start_at` (`start_at`),
  KEY `idx_booking_status_start` (`status`,`start_at`),
  KEY `idx_booking_email` (`email`),
  KEY `fk_booking_product` (`product_id`),
  CONSTRAINT `fk_booking_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `booking_day_lock` (
  `lock_date` date NOT NULL,
  PRIMARY KEY (`lock_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `booking_option` (
  `booking_id` bigint NOT NULL,
  `option_id` bigint DEFAULT NULL,
  `option_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `quantity` int NOT NULL,
  `unit_price` decimal(12,0) NOT NULL,
  KEY `fk_booking_option_booking` (`booking_id`),
  CONSTRAINT `fk_booking_option_booking` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `competition` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `country` enum('JP','KR') COLLATE utf8mb4_unicode_ci NOT NULL,
  `end_date` date NOT NULL,
  `link` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `published` bit(1) NOT NULL,
  `start_date` date NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_competition_start` (`start_date`),
  KEY `idx_competition_country` (`country`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `competition_translation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` text COLLATE utf8mb4_unicode_ci,
  `host` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `locale` enum('JA','KO') COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `place` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `competition_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_translation` (`competition_id`,`locale`),
  CONSTRAINT `fk_competition_translation_competition` FOREIGN KEY (`competition_id`) REFERENCES `competition` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `gallery_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `category` enum('INTERPRETER','PHOTO','VIDEO') COLLATE utf8mb4_unicode_ci NOT NULL,
  `consent` bit(1) NOT NULL,
  `consent_note` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image_key` varchar(300) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ratio` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int NOT NULL,
  `taken_at` date DEFAULT NULL,
  `thumb_key` varchar(300) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_gallery_consent_sort` (`consent`,`sort_order`),
  KEY `idx_gallery_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `gallery_translation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `caption` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `locale` enum('JA','KO') COLLATE utf8mb4_unicode_ci NOT NULL,
  `gallery_item_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_gallery_translation` (`gallery_item_id`,`locale`),
  CONSTRAINT `fk_gallery_translation_item` FOREIGN KEY (`gallery_item_id`) REFERENCES `gallery_item` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `inquiry` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `admin_memo` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `handled_at` datetime(6) DEFAULT NULL,
  `handled_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `locale` enum('JA','KO') COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `privacy_agreed` bit(1) NOT NULL,
  `status` enum('HANDLED','PENDING') COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` enum('INTERPRETER','VIDEO') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_inquiry_status_created` (`status`,`created_at`),
  KEY `idx_inquiry_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `category` enum('MEDIA','NOTICE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `published` bit(1) NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `slug` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `thumbnail_key` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `view_count` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_slug` (`slug`),
  KEY `idx_post_published` (`published`,`published_at`),
  KEY `idx_post_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `post_translation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `body` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `excerpt` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `locale` enum('JA','KO') COLLATE utf8mb4_unicode_ci NOT NULL,
  `series` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `post_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_translation` (`post_id`,`locale`),
  CONSTRAINT `fk_post_translation_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `active` bit(1) NOT NULL,
  `bookable` bit(1) NOT NULL,
  `currency` varchar(3) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description_ja` text COLLATE utf8mb4_unicode_ci,
  `description_ko` text COLLATE utf8mb4_unicode_ci,
  `duration_min` int NOT NULL,
  `name_ja` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name_ko` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `note_ja` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `note_ko` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `price` decimal(12,0) NOT NULL,
  `price_unit` enum('PER_DAY','PER_HOUR','PER_SESSION') COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int NOT NULL,
  `type` enum('INTERPRETER','PHOTO','VIDEO') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `product_include` (
  `product_id` bigint NOT NULL,
  `content` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `locale` enum('JA','KO') COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int NOT NULL,
  KEY `fk_product_include_product` (`product_id`),
  CONSTRAINT `fk_product_include_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `product_option` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `max_quantity` int NOT NULL,
  `name_ja` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name_ko` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `price` decimal(12,0) NOT NULL,
  `sort_order` int NOT NULL,
  `product_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_product_option_product` (`product_id`),
  CONSTRAINT `fk_product_option_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `refresh_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_user_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `revoked_at` datetime(6) DEFAULT NULL,
  `token_hash` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_token_hash` (`token_hash`),
  KEY `idx_refresh_token_admin` (`admin_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
