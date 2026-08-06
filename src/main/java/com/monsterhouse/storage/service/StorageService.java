package com.monsterhouse.storage.service;

/**
 * 파일 저장소 추상화.
 *
 * 구현을 둘로 나눈 이유:
 *   운영은 S3 + CloudFront 지만(기획서 §7.1), 개발자가 AWS 자격증명 없이도
 *   갤러리 업로드를 만지고 테스트할 수 있어야 합니다.
 *   app.storage.type 으로 갈아끼웁니다.
 *
 * 저장하는 값은 URL 이 아니라 key 입니다.
 * URL 을 DB 에 박으면 CDN 도메인이 바뀌는 순간 과거 데이터가 전부 깨집니다.
 */
public interface StorageService {

    /**
     * @param key         저장 경로 (예: gallery/2026/08/abc123_thumb.jpg)
     * @param contentType image/jpeg 등
     */
    void store(String key, byte[] content, String contentType);

    void delete(String key);

    /** 공개 URL. key 가 null 이면 null 을 돌려줍니다(썸네일 없는 콘텐츠 대응). */
    String url(String key);
}
