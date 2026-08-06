package com.monsterhouse.content.gallery.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.monsterhouse.booking.entity.ProductType;
import com.monsterhouse.common.enums.LocaleCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record GallerySaveRequest(

        @NotNull
        ProductType category,

        /** 업로드 API 가 돌려준 키 */
        @NotBlank
        @Size(max = 300)
        String imageKey,

        @NotBlank
        @Size(max = 300)
        String thumbKey,

        @NotBlank
        @Size(max = 20)
        String ratio,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate takenAt,

        /**
         * 게시 동의 (기획서 §6.2).
         * 요청에 명시하지 않으면 false — 즉 기본은 비공개입니다.
         */
        boolean consent,

        @Size(max = 300)
        String consentNote,

        int sortOrder,

        @Valid
        List<Translation> translations
) {

    public record Translation(

            @NotNull
            LocaleCode locale,

            @Size(max = 300)
            String caption
    ) {
    }
}
