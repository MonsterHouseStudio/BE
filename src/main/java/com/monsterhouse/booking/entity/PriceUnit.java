package com.monsterhouse.booking.entity;

/**
 * 가격 단위.
 *
 * 통역은 "100,000원/1일", "50,000원/1시간" 처럼 단위가 붙습니다.
 * 단위를 문자열로 상품명에 섞어 적으면 다국어 표기를 만들 수 없어서 별도 값으로 둡니다.
 */
public enum PriceUnit {

    /** 1회 촬영 기준 */
    PER_SESSION,
    PER_DAY,
    PER_HOUR
}
