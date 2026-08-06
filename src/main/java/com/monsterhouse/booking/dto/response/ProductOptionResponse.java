package com.monsterhouse.booking.dto.response;

import com.monsterhouse.booking.entity.ProductOption;
import com.monsterhouse.common.enums.LocaleCode;

import java.math.BigDecimal;

public record ProductOptionResponse(
        Long id,
        String name,
        BigDecimal price,
        /** 1 이면 체크박스, 2 이상이면 수량 선택으로 그립니다. */
        int maxQuantity
) {

    public static ProductOptionResponse of(ProductOption option, LocaleCode locale) {
        return new ProductOptionResponse(
                option.getId(),
                option.name(locale),
                option.getPrice(),
                option.getMaxQuantity()
        );
    }
}
