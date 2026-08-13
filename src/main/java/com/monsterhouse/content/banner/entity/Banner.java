package com.monsterhouse.content.banner.entity;

import com.monsterhouse.common.entity.BaseTimeEntity;
import com.monsterhouse.common.enums.LocaleCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "banner")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Banner extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private BannerMediaType mediaType;

    @Column(name = "media_key", nullable = false, length = 300)
    private String mediaKey;

    @Column(name = "poster_key", length = 300)
    private String posterKey;

    @Column(name = "headline_ko", length = 200)
    private String headlineKo;
    @Column(name = "headline_ja", length = 200)
    private String headlineJa;
    @Column(name = "subtext_ko", length = 500)
    private String subtextKo;
    @Column(name = "subtext_ja", length = 500)
    private String subtextJa;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder
    private Banner(BannerMediaType mediaType, String mediaKey, String posterKey,
                   String headlineKo, String headlineJa,
                   String subtextKo, String subtextJa,
                   boolean active, int sortOrder) {
        this.mediaType = mediaType;
        this.mediaKey = mediaKey;
        this.posterKey = posterKey;
        this.headlineKo = headlineKo;
        this.headlineJa = headlineJa;
        this.subtextKo = subtextKo;
        this.subtextJa = subtextJa;
        this.active = active;
        this.sortOrder = sortOrder;
    }

    public void update(BannerMediaType mediaType, String mediaKey, String posterKey,
                       String headlineKo, String headlineJa,
                       String subtextKo, String subtextJa, int sortOrder) {
        this.mediaType = mediaType;
        this.mediaKey = mediaKey;
        this.posterKey = posterKey;
        this.headlineKo = headlineKo;
        this.headlineJa = headlineJa;
        this.subtextKo = subtextKo;
        this.subtextJa = subtextJa;
        this.sortOrder = sortOrder;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    public String headline(LocaleCode locale) {
        if (locale == LocaleCode.JA && headlineJa != null && !headlineJa.isBlank()) {
            return headlineJa;
        }
        return headlineKo;
    }

    public String subtext(LocaleCode locale) {
        if (locale == LocaleCode.JA && subtextJa != null && !subtextJa.isBlank()) {
            return subtextJa;
        }
        return subtextKo;
    }
}