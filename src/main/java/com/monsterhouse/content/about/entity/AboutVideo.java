package com.monsterhouse.content.about.entity;

import com.monsterhouse.common.entity.BaseTimeEntity;
import com.monsterhouse.common.enums.LocaleCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 소개 페이지 "최신 영상". 유튜브 URL + 썸네일 + 제목(한/일). */
@Getter
@Entity
@Table(name = "about_video")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AboutVideo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "youtube_url", length = 500, nullable = false)
    private String youtubeUrl;

    @Column(name = "title_ko", length = 200)
    private String titleKo;
    @Column(name = "title_ja", length = 200)
    private String titleJa;

    /** 썸네일 이미지 키(없으면 프론트가 유튜브 기본 썸네일로 대체). */
    @Column(name = "thumbnail_key", length = 300)
    private String thumbnailKey;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder
    private AboutVideo(String youtubeUrl, String titleKo, String titleJa,
                       String thumbnailKey, boolean active, int sortOrder) {
        this.youtubeUrl = youtubeUrl;
        this.titleKo = titleKo;
        this.titleJa = titleJa;
        this.thumbnailKey = thumbnailKey;
        this.active = active;
        this.sortOrder = sortOrder;
    }

    public void update(String youtubeUrl, String titleKo, String titleJa,
                       String thumbnailKey, int sortOrder) {
        this.youtubeUrl = youtubeUrl;
        this.titleKo = titleKo;
        this.titleJa = titleJa;
        this.thumbnailKey = thumbnailKey;
        this.sortOrder = sortOrder;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String title(LocaleCode locale) {
        if (locale == LocaleCode.JA && titleJa != null && !titleJa.isBlank()) return titleJa;
        return titleKo;
    }
}
