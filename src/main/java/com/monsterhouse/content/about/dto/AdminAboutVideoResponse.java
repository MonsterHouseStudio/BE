package com.monsterhouse.content.about.dto;

import com.monsterhouse.content.about.entity.AboutVideo;

public record AdminAboutVideoResponse(
        Long id,
        String youtubeUrl,
        String titleKo,
        String titleJa,
        String thumbnailKey,
        String thumbnailUrl,
        boolean active,
        int sortOrder
) {
    public static AdminAboutVideoResponse of(AboutVideo v, String thumbnailUrl) {
        return new AdminAboutVideoResponse(
                v.getId(),
                v.getYoutubeUrl(),
                v.getTitleKo(), v.getTitleJa(),
                v.getThumbnailKey(), thumbnailUrl,
                v.isActive(), v.getSortOrder()
        );
    }
}
