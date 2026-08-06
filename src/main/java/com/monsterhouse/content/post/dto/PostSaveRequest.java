package com.monsterhouse.content.post.dto;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.content.post.entity.PostCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record PostSaveRequest(

        /** URL 에 쓰이므로 영문 소문자·숫자·하이픈만 허용합니다. */
        @NotBlank
        @Size(max = 120)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "slug 는 영문 소문자, 숫자, 하이픈만 사용할 수 있습니다.")
        String slug,

        @NotNull
        PostCategory category,

        /** 업로드 API 가 돌려준 thumbKey 또는 mediumKey */
        @Size(max = 300)
        String thumbnailKey,

        boolean published,

        @NotEmpty
        @Valid
        List<Translation> translations
) {

    public record Translation(

            @NotNull
            LocaleCode locale,

            @Size(max = 100)
            String series,

            @NotBlank
            @Size(max = 200)
            String title,

            @Size(max = 500)
            String excerpt,

            @NotBlank
            String body
    ) {
    }
}
