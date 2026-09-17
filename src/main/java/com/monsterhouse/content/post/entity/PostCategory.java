package com.monsterhouse.content.post.entity;

/**
 * 미디어 글(ARTICLE) 카테고리.
 *
 * MEDIA 는 초기 버전의 포괄 카테고리로, V5 마이그레이션에서 기존 행을 CREW 로 옮겼습니다.
 * 열거값 자체는 하위호환을 위해 남겨둡니다(신규 작성에는 쓰지 않음).
 * SNS 종류의 글은 카테고리가 의미 없어 ETC 로 저장합니다.
 */
public enum PostCategory {

    /** 협찬사 소개 */
    SPONSOR,
    /** 스토리 */
    STORY,
    /** 크루 이야기 (구 크루 성장기) */
    CREW,
    /** 기타 */
    ETC,
    /** 공지 */
    NOTICE,

    /** @deprecated 초기 포괄 카테고리. 신규 작성 금지. */
    @Deprecated
    MEDIA
}
