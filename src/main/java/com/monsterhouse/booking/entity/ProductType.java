package com.monsterhouse.booking.entity;

/**
 * 촬영 상품 분류.
 *
 * 갤러리 카테고리로도 재사용합니다(GalleryItem.category).
 * 분류를 두 곳에서 따로 관리하면 상품이 늘 때마다 양쪽을 고쳐야 합니다.
 */
public enum ProductType {

    /** 사진 촬영 — 보정본 장수로 상품이 나뉩니다 */
    PHOTO,
    /** 영상 촬영 — 포징영상 / 모티베이션 / 묶음 */
    VIDEO,
    /**
     * 통역 — 대회 서포트, PT 통역.
     * 대회 일정에 맞춰야 해서 스튜디오 시간표로 잡을 수 없으므로
     * bookable = false 로 두고 가격표만 노출합니다(예약은 문의 폼으로).
     */
    INTERPRETER
}
