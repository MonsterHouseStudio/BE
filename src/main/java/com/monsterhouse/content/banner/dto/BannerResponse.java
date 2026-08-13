package com.monsterhouse.content.banner.dto;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.content.banner.entity.Banner;
import com.monsterhouse.content.banner.entity.BannerMediaType;
public record BannerResponse(
        Long id,
        BannerMediaType mediaType,
        String mediaUrl,
        String posterUrl,
        String headline,
        String subtext
) {
    public static BannerResponse of(Banner banner, LocaleCode locale, String mediaUrl, String posterUrl){
        return new BannerResponse(
                banner.getId(),
                banner.getMediaType(),
                mediaUrl,
                posterUrl,
                banner.headline(locale),
                banner.subtext(locale)
        );
    }
}
