package com.monsterhouse.booking.dto.response;

import com.monsterhouse.booking.entity.ProductOption;

import java.math.BigDecimal;

/**
 * 관리자 편집 화면 전용 옵션 응답.
 *
 * 고객용 {@link ProductOptionResponse} 는 로케일 한 벌(name)만 내려주지만,
 * 관리자는 한국어·일본어를 각각 편집해야 하므로 폴백 없이 원본 값을 그대로 내려줍니다.
 * (nameJa 가 비어 있으면 편집 화면에서도 비어 있어야 합니다 — 한국어가 일본어인 척
 *  채워지면 그대로 저장 시 한국어가 일본어 칸에 박히기 때문.)
 */
public record AdminProductOptionResponse(
        Long id,
        String nameKo,
        String nameJa,
        BigDecimal price,
        int maxQuantity,
        int sortOrder,
        boolean active
) {
    public static AdminProductOptionResponse of(ProductOption option) {
        return new AdminProductOptionResponse(
                option.getId(),
                option.getNameKo(),
                option.getNameJa(),
                option.getPrice(),
                option.getMaxQuantity(),
                option.getSortOrder(),
                option.isActive()
        );
    }
}
