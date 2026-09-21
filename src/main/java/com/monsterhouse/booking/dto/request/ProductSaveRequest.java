package com.monsterhouse.booking.dto.request;

import com.monsterhouse.booking.entity.ProductType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record ProductSaveRequest(
        @NotNull
        ProductType type,
        @NotBlank
        @Size(max = 100)
        String nameKo,
        @Size(max = 100)
        String nameJa,
        String descriptionKo,
        String descriptionJa,
        @Min(15)
        @Max(600)
        int durationMin,
        @NotNull
        @DecimalMin("0")
        BigDecimal price,
        List<@Size(max = 200) String>includesKo,
        List<@Size(max = 200) String>includesJa,
        @Min(0)
        int sortOrder,

        /** 가격 단위. 미지정이면 1회 기준(PER_SESSION). */
        com.monsterhouse.booking.entity.PriceUnit priceUnit,

        /**
         * 온라인 슬롯 예약 대상인가.
         * 통역처럼 대회 일정에 맞춰야 하는 상품은 false 로 두고 가격표만 노출합니다.
         */
        boolean bookable,

        @Size(max = 300)
        String noteKo,

        @Size(max = 300)
        String noteJa,

        /** 상품 대표 이미지 키(업로드 후 받은 mediumKey). 없으면 이미지 없음. */
        @Size(max = 300)
        String imageKey
) {
}
