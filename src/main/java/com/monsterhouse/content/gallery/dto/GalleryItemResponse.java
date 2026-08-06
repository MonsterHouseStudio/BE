package com.monsterhouse.content.gallery.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.monsterhouse.booking.entity.ProductType;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.content.gallery.entity.GalleryItem;

import java.time.LocalDate;

public record GalleryItemResponse(
        Long id,
        ProductType category,
        String caption,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate takenAt,

        /** 목록 그리드에는 썸네일, 라이트박스에는 원본을 씁니다. */
        String thumbUrl,
        String imageUrl,
        String ratio
) {

    public static GalleryItemResponse of(GalleryItem item, LocaleCode locale,
                                         String thumbUrl, String imageUrl) {
        return new GalleryItemResponse(
                item.getId(),
                item.getCategory(),
                item.caption(locale),
                item.getTakenAt(),
                thumbUrl,
                imageUrl,
                item.getRatio()
        );
    }
}
