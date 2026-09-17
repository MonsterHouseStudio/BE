-- 미디어센터: 콘텐츠 종류(글/SNS)와 카테고리 확장.
-- category 는 ENUM 이라 값 추가에 MODIFY 가 필요합니다.

-- 1) 카테고리 값 확장 (협찬사/스토리/크루 이야기/기타 추가, 기존 MEDIA/NOTICE 유지)
ALTER TABLE `post`
    MODIFY COLUMN `category`
    ENUM('MEDIA','NOTICE','SPONSOR','STORY','CREW','ETC')
    COLLATE utf8mb4_unicode_ci NOT NULL;

-- 2) 종류(글/SNS) 컬럼 — 기존 글은 전부 ARTICLE
ALTER TABLE `post`
    ADD COLUMN `kind` ENUM('ARTICLE','SNS')
    COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ARTICLE' AFTER `category`;

-- 3) SNS 전용 외부 링크
ALTER TABLE `post`
    ADD COLUMN `link_url` VARCHAR(500)
    COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `thumbnail_key`;

-- 4) 기존 포괄 카테고리(MEDIA) 글을 '크루 이야기'(CREW)로 이관
UPDATE `post` SET `category` = 'CREW' WHERE `category` = 'MEDIA';
