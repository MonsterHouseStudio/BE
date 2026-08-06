package com.monsterhouse.storage.dto;

/**
 * 업로드 결과. DB 에는 key 를 저장하고, 응답에는 url 을 함께 실어
 * 관리자 화면이 바로 미리보기를 띄울 수 있게 합니다.
 */
public record UploadedImage(
        String originalKey,
        String mediumKey,
        String thumbKey,
        String originalUrl,
        String mediumUrl,
        String thumbUrl,
        int width,
        int height,
        /** portrait | landscape | square — 갤러리 매스너리 레이아웃에 씁니다 */
        String ratio
) {
}
