package com.monsterhouse.booking.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record BookingCreateRequest(

        @NotNull(message = "{valid.name.required}")
        Long productId,

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime startAt,

        @NotBlank(message = "{valid.name.required}")
        @Size(max = 50)
        String name,

        @NotBlank(message = "{valid.phone.required}")
        @Size(max = 30)
        @Pattern(regexp = "^[0-9+\\-() ]{7,30}$", message = "{valid.phone.pattern}")
        String phone,

        @NotBlank(message = "{valid.email.required}")
        @Email(message = "{valid.email.format}")
        @Size(max = 200)
        String email,

        @Size(max = 2000)
        String memo,

        /** 기획서 §9 — 개인정보 수집·이용 동의 없이는 접수 불가 */
        @AssertTrue(message = "{valid.privacy.required}")
        boolean privacyAgreed,

        /**
         * 선택한 추가 옵션. 없으면 비워도 됩니다.
         * 가격은 서버가 DB 에서 다시 조회해 계산합니다 — 클라이언트가 보낸 금액은 신뢰하지 않습니다.
         */
        @jakarta.validation.Valid
        java.util.List<OptionSelection> options,

        /**
         * 스팸 방지 honeypot (기획서 §9).
         * 사람에게는 보이지 않는 필드라 값이 차 있으면 봇입니다.
         */
        String website
) {

        public record OptionSelection(
                @NotNull Long optionId,
                @Min(1) int quantity
        ) {
        }
    public boolean isBot() {
        return website != null && !website.isBlank();
    }
}