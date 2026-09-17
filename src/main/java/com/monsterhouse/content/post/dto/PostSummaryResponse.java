package com.monsterhouse.content.post.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.content.post.entity.Post;
import com.monsterhouse.content.post.entity.PostCategory;
import com.monsterhouse.content.post.entity.PostKind;
import com.monsterhouse.content.post.entity.PostTranslation;

import java.time.LocalDateTime;

/** 목록 카드용. 본문(body)은 싣지 않습니다 — 목록에서 쓰지도 않는데 응답만 무거워집니다. */
public record PostSummaryResponse(
        Long id,
        String slug,
        PostKind kind,
        PostCategory category,
        String series,
        String title,
        String excerpt,
        String thumbnailUrl,
        /** SNS 카드는 이 링크로 외부 이동합니다. ARTICLE 은 null(상세 페이지로). */
        String linkUrl,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime publishedAt,

        long viewCount
) {

    public static PostSummaryResponse of(Post post, LocaleCode locale, String thumbnailUrl) {
        PostTranslation t = post.findTranslation(locale)
                .orElseThrow(() -> new IllegalStateException(
                        "번역이 없는 글이 공개 목록에 들어왔습니다. id=" + post.getId()));

        return new PostSummaryResponse(
                post.getId(),
                post.getSlug(),
                post.getKind(),
                post.getCategory(),
                t.getSeries(),
                t.getTitle(),
                t.getExcerpt(),
                thumbnailUrl,
                post.getLinkUrl(),
                post.getPublishedAt(),
                post.getViewCount()
        );
    }
}
