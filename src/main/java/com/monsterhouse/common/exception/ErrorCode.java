package com.monsterhouse.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * code    : 프론트가 분기에 쓰는 안정적인 식별자 (메시지 문구가 바뀌어도 불변)
 * status  : HTTP 상태
 * messageKey : messages_{ko,ja}.properties 의 키. 실제 문구는 요청 locale 로 결정.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ===== 공통 =====
    INTERNAL_ERROR("C001", HttpStatus.INTERNAL_SERVER_ERROR, "error.internal"),
    INVALID_INPUT("C002", HttpStatus.BAD_REQUEST, "error.invalid.input"),
    NOT_FOUND("C003", HttpStatus.NOT_FOUND, "error.not.found"),
    UNAUTHORIZED("C004", HttpStatus.UNAUTHORIZED, "error.unauthorized"),
    FORBIDDEN("C005", HttpStatus.FORBIDDEN, "error.forbidden"),
    TOO_MANY_REQUESTS("C006", HttpStatus.TOO_MANY_REQUESTS, "error.too.many.requests"),
    DUPLICATE_RESOURCE("C007", HttpStatus.CONFLICT, "error.invalid.input"),

    // ===== 예약 =====
    SLOT_ALREADY_TAKEN("B001", HttpStatus.CONFLICT, "error.booking.slot.taken"),
    SLOT_UNAVAILABLE("B002", HttpStatus.BAD_REQUEST, "error.booking.slot.unavailable"),
    CLOSED_DAY("B003", HttpStatus.BAD_REQUEST, "error.booking.closed.day"),
    OUT_OF_BOOKING_RANGE("B004", HttpStatus.BAD_REQUEST, "error.booking.out.of.range"),
    BOOKING_TOO_LATE("B005", HttpStatus.BAD_REQUEST, "error.booking.too.late"),
    BOOKING_NOT_FOUND("B006", HttpStatus.NOT_FOUND, "error.booking.not.found"),
    INVALID_BOOKING_STATUS("B007", HttpStatus.BAD_REQUEST, "error.booking.invalid.status"),
    ALREADY_CANCELED("B008", HttpStatus.BAD_REQUEST, "error.booking.already.canceled"),
    PRODUCT_NOT_FOUND("B009", HttpStatus.NOT_FOUND, "error.product.not.found"),
    PRODUCT_INACTIVE("B010", HttpStatus.BAD_REQUEST, "error.product.inactive"),
    PRODUCT_IN_USE("B011", HttpStatus.CONFLICT, "error.product.in.use"),
    PRODUCT_NOT_BOOKABLE("B012", HttpStatus.BAD_REQUEST, "error.product.not.bookable"),
    INVALID_OPTION("B013", HttpStatus.BAD_REQUEST, "error.booking.invalid.option"),
    // ===== 콘텐츠 =====
    POST_NOT_FOUND("P001", HttpStatus.NOT_FOUND, "error.post.not.found"),
    TRANSLATION_MISSING("P002", HttpStatus.NOT_FOUND, "error.translation.missing"),

    // ===== 문의 =====
    INQUIRY_NOT_FOUND("I001", HttpStatus.NOT_FOUND, "error.inquiry.not.found"),

    // ===== 관리자 =====
    INVALID_CREDENTIALS("A001", HttpStatus.UNAUTHORIZED, "error.auth.invalid.credentials"),
    TOKEN_EXPIRED("A002", HttpStatus.UNAUTHORIZED, "error.auth.token.expired"),
    INVALID_TOKEN("A003", HttpStatus.UNAUTHORIZED, "error.auth.token.invalid"),
    ACCOUNT_LOCKED("A004", HttpStatus.LOCKED, "error.auth.account.locked"),
    REFRESH_TOKEN_INVALID("A005", HttpStatus.UNAUTHORIZED, "error.auth.refresh.invalid"),

    DUPLICATE_SLUG("P003", HttpStatus.CONFLICT, "error.post.duplicate.slug"),

    // ===== 파일 업로드 =====
    FILE_TOO_LARGE("F001", HttpStatus.PAYLOAD_TOO_LARGE, "error.file.too.large"),
    UNSUPPORTED_FILE_TYPE("F002", HttpStatus.BAD_REQUEST, "error.file.unsupported");
    private final String code;
    private final HttpStatus status;
    private final String messageKey;
}