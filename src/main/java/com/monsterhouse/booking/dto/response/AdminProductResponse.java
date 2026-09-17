package com.monsterhouse.booking.dto.response;

import com.monsterhouse.booking.entity.Product;
import com.monsterhouse.booking.entity.ProductOption;
import com.monsterhouse.booking.entity.ProductType;
import com.monsterhouse.common.enums.LocaleCode;

import java.math.BigDecimal;
import java.util.List;

public record AdminProductResponse(
        Long id,
        ProductType type,
        String nameKo,
        String nameJa,
        String descriptionKo,
        String descriptionJa,
        int durationMin,
        BigDecimal price,
        String currency,
        List<String> includesKo,
        List<String> includesJa,
        boolean active,
        int sortOrder,
        com.monsterhouse.booking.entity.PriceUnit priceUnit,
        boolean bookable,
        String noteKo,
        String noteJa,
        List<AdminProductOptionResponse> options,
        boolean translated
) {
    public static AdminProductResponse of(Product product){
        String nameJa = product.getNameJa();
        boolean translated = nameJa != null && !nameJa.isBlank();
        return new AdminProductResponse(
                product.getId(),
                product.getType(),
                product.getNameKo(),
                nameJa,
                product.getDescriptionKo(),
                product.getDescriptionJa(),
                product.getDurationMin(),
                product.getPrice(),
                product.getCurrency(),
                // 폴백 없는 rawIncludes — 편집 화면에 한국어가 일본어인 척 들어가면 안 됩니다.
                product.rawIncludes(LocaleCode.KO),
                product.rawIncludes(LocaleCode.JA),
                product.isActive(),
                product.getSortOrder(),
                product.getPriceUnit(),
                product.isBookable(),
                product.getNoteKo(),
                product.getNoteJa(),
                product.getOptions().stream()
                        .sorted(java.util.Comparator.comparingInt(ProductOption::getSortOrder))
                        .map(AdminProductOptionResponse::of)
                        .toList(),
                translated
        );
    }
}
