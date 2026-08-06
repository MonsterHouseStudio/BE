package com.monsterhouse.content.post.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.content.post.entity.Post;
import com.monsterhouse.content.post.entity.PostTranslation;

import java.time.LocalDateTime;

public record PostDetailResponse(
        Long id,
        String slug,
        String series,
        String title,
        String excerpt,
        String body,
        String thumbnailUrl,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime publishedAt,

        long viewCount
) {

    public static PostDetailResponse of(Post post, LocaleCode locale, String thumbnailUrl) {
        PostTranslation t = post.findTranslation(locale)
                .orElseThrow(() -> new IllegalStateException(
                        "번역이 없는 글이 조회되었습니다. id=" + post.getId()));

        return new PostDetailResponse(
                post.getId(),
                post.getSlug(),
                t.getSeries(),
                t.getTitle(),
                t.getExcerpt(),
                t.getBody(),
                thumbnailUrl,
                post.getPublishedAt(),
                post.getViewCount()
        );
    }
}
