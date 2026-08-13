package com.monsterhouse.content.banner.dto;

import com.monsterhouse.content.banner.entity.Banner;
import com.monsterhouse.content.banner.entity.BannerMediaType;

public record AdminBannerResponse(
        Long id,
        BannerMediaType mediaType,
        String mediaKey,
        String mediaUrl,
        String posterKey,
        String posterUrl,
        String headlineKo,
        String headlineJa,
        String subtextKo,
        String subtextJa,
        boolean active,
        int sortOrder
) {
    public static AdminBannerResponse of(Banner banner, String mediaUrl, String posterUrl) {
        return new AdminBannerResponse(
                banner.getId(),
                banner.getMediaType(),
                banner.getMediaKey(),
                mediaUrl,
                banner.getPosterKey(),
                posterUrl,
                banner.getHeadlineKo(),
                banner.getHeadlineJa(),
                banner.getSubtextKo(),
                banner.getSubtextJa(),
                banner.isActive(),
                banner.getSortOrder()
            );
    }
}