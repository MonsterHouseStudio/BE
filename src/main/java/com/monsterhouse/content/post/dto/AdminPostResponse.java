package com.monsterhouse.content.post.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.content.post.entity.Post;
import com.monsterhouse.content.post.entity.PostCategory;
import com.monsterhouse.content.post.entity.PostKind;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record AdminPostResponse(
        Long id,
        String slug,
        PostKind kind,
        PostCategory category,
        String thumbnailKey,
        String thumbnailUrl,
        String linkUrl,
        boolean published,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime publishedAt,

        long viewCount,
        List<PostSaveRequest.Translation> translations,
        /** 프론트의 "일본어 미작성" 배지용 (기획서 §3.2) */
        List<LocaleCode> missingLocales
) {

    public static AdminPostResponse of(Post post, String thumbnailUrl) {
        List<PostSaveRequest.Translation> translations = post.getTranslations().stream()
                .map(t -> new PostSaveRequest.Translation(
                        t.getLocale(), t.getSeries(), t.getTitle(), t.getExcerpt(), t.getBody()))
                .toList();

        List<LocaleCode> missing = Arrays.stream(LocaleCode.values())
                .filter(locale -> !post.hasTranslation(locale))
                .toList();

        return new AdminPostResponse(
                post.getId(),
                post.getSlug(),
                post.getKind(),
                post.getCategory(),
                post.getThumbnailKey(),
                thumbnailUrl,
                post.getLinkUrl(),
                post.isPublished(),
                post.getPublishedAt(),
                post.getViewCount(),
                translations,
                missing
        );
    }
}
