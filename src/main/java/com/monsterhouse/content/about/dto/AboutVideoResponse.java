package com.monsterhouse.content.about.dto;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.content.about.entity.AboutVideo;

public record AboutVideoResponse(
        Long id,
        String youtubeUrl,
        String title,
        String thumbnailUrl
) {
    public static AboutVideoResponse of(AboutVideo v, LocaleCode locale, String thumbnailUrl) {
        return new AboutVideoResponse(
                v.getId(),
                v.getYoutubeUrl(),
                v.title(locale),
                thumbnailUrl
        );
    }
}
