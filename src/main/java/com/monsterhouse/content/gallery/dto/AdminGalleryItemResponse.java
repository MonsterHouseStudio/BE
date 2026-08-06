package com.monsterhouse.content.gallery.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.monsterhouse.booking.entity.ProductType;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.content.gallery.entity.GalleryItem;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public record AdminGalleryItemResponse(
        Long id,
        ProductType category,
        String imageKey,
        String thumbKey,
        String imageUrl,
        String thumbUrl,
        String ratio,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate takenAt,

        boolean consent,
        String consentNote,
        int sortOrder,
        List<GallerySaveRequest.Translation> translations,
        List<LocaleCode> missingLocales
) {

    public static AdminGalleryItemResponse of(GalleryItem item, String imageUrl, String thumbUrl) {
        List<GallerySaveRequest.Translation> translations = item.getTranslations().stream()
                .map(t -> new GallerySaveRequest.Translation(t.getLocale(), t.getCaption()))
                .toList();

        List<LocaleCode> missing = Arrays.stream(LocaleCode.values())
                .filter(locale -> item.findTranslation(locale).isEmpty())
                .toList();

        return new AdminGalleryItemResponse(
                item.getId(),
                item.getCategory(),
                item.getImageKey(),
                item.getThumbKey(),
                imageUrl,
                thumbUrl,
                item.getRatio(),
                item.getTakenAt(),
                item.isConsent(),
                item.getConsentNote(),
                item.getSortOrder(),
                translations,
                missing
        );
    }
}
