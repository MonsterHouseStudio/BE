package com.monsterhouse.content.post.entity;

/**
 * 미디어 콘텐츠 종류.
 *
 * 관리자 작성 화면에서 라디오로 고르며, 종류에 따라 입력 양식이 달라집니다.
 * - ARTICLE : 제목·본문·썸네일 + 카테고리(협찬사/스토리/크루 이야기/기타). 상세 페이지가 있습니다.
 * - SNS     : 유튜브 URL + 썸네일 + 제목. 카드 클릭 시 외부(유튜브)로 이동하며 본문은 없습니다.
 */
public enum PostKind {
    ARTICLE,
    SNS
}
