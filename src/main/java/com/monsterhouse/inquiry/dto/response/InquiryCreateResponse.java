package com.monsterhouse.inquiry.dto.response;

/**
 * 접수 확인용 최소 응답.
 * 프론트(api.ts createInquiry)가 { id } 를 기대합니다.
 * 신청자에게 다른 사람의 문의를 조회할 통로를 주지 않기 위해 이 이상은 내리지 않습니다.
 */
public record InquiryCreateResponse(Long id) {
}