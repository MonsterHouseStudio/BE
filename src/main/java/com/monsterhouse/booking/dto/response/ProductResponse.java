package com.monsterhouse.booking.dto.response;

import com.monsterhouse.booking.entity.PriceUnit;
import com.monsterhouse.booking.entity.Product;
import com.monsterhouse.booking.entity.ProductType;
import com.monsterhouse.common.enums.LocaleCode;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        Long id,
        ProductType type,
        String name,
        String description,
        int durationMin,
        BigDecimal price,
        String currency,
        /** PER_SESSION / PER_DAY / PER_HOUR — 프론트가 "/1일" 같은 접미사를 붙입니다 */
        PriceUnit priceUnit,
        /** false 면 예약 버튼 대신 문의 안내를 띄웁니다 (통역) */
        boolean bookable,
        /** "* 사진촬영 별도" 처럼 가격 옆에 붙는 단서 */
        String note,
        /** 상품 대표 이미지 URL(없으면 null) */
        String imageUrl,
        /** 프론트 ShootingPage 의 "포함 사항" 목록 */
        List<String> includes,
        /** 추가 옵션 (보정본 추가 등) */
        List<ProductOptionResponse> options
) {

    public static ProductResponse of(Product product, LocaleCode locale, String imageUrl) {
        return new ProductResponse(
                product.getId(),
                product.getType(),
                product.name(locale),
                product.description(locale),
                product.getDurationMin(),
                product.getPrice(),
                product.getCurrency(),
                product.getPriceUnit(),
                product.isBookable(),
                product.note(locale),
                imageUrl,
                product.includes(locale),
                product.activeOptions().stream()
                        .map(option -> ProductOptionResponse.of(option, locale))
                        .toList()
        );
    }
}
