package com.monsterhouse.content.post.entity;

import com.monsterhouse.common.entity.BaseTimeEntity;
import com.monsterhouse.common.enums.LocaleCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 미디어 글 (기획서 §6.4, §3.2).
 *
 * ★ 이 구조가 기획서 §3.2 의 핵심입니다.
 *   규칙 A: 같은 글의 번역   → post 하나에 post_translation 2개(ko/ja)
 *   규칙 B: 언어별 독립 콘텐츠 → post 를 각각 만들고 해당 locale 번역만 등록
 *
 *   즉 "한국 크루 성장기"와 "일본 크루 성장기"는 서로 다른 post 입니다.
 *   번역이 아니라 별개의 글이기 때문입니다.
 *
 * slug 는 언어와 무관하게 글 하나를 가리킵니다(/ko/media/x, /ja/media/x 가 같은 글).
 */
@Getter
@Entity
@Table(
        name = "post",
        uniqueConstraints = @UniqueConstraint(name = "uk_post_slug", columnNames = "slug"),
        indexes = {
                @Index(name = "idx_post_published", columnList = "published, published_at"),
                @Index(name = "idx_post_category", columnList = "category")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slug", nullable = false, length = 120)
    private String slug;

    /** 콘텐츠 종류(글/SNS). 종류에 따라 유효 필드가 달라집니다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private PostKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private PostCategory category;

    /** 스토리지 키. URL 이 아니라 키를 저장해야 CDN 도메인이 바뀌어도 데이터가 안 썩습니다. */
    @Column(name = "thumbnail_key", length = 300)
    private String thumbnailKey;

    /** SNS 종류 전용 — 유튜브 등 외부 링크. ARTICLE 은 null. */
    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostTranslation> translations = new ArrayList<>();

    @Builder
    private Post(String slug, PostKind kind, PostCategory category,
                String thumbnailKey, String linkUrl, boolean published) {
        this.slug = slug;
        this.kind = kind == null ? PostKind.ARTICLE : kind;
        this.category = category;
        this.thumbnailKey = thumbnailKey;
        this.linkUrl = linkUrl;
        this.published = published;
        this.viewCount = 0L;
        if (published) {
            this.publishedAt = LocalDateTime.now();
        }
    }

    public void update(String slug, PostKind kind, PostCategory category,
                       String thumbnailKey, String linkUrl, boolean published) {
        this.slug = slug;
        this.kind = kind == null ? PostKind.ARTICLE : kind;
        this.category = category;
        this.thumbnailKey = thumbnailKey;
        this.linkUrl = linkUrl;

        // 최초 공개 시점만 기록합니다. 수정할 때마다 갱신하면
        // 목록 정렬이 흔들려 오래된 글이 갑자기 맨 위로 올라옵니다.
        if (published && this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
        this.published = published;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void putTranslation(LocaleCode locale, String series, String title,
                               String excerpt, String body) {
        findTranslation(locale).ifPresentOrElse(
                t -> t.update(series, title, excerpt, body),
                () -> this.translations.add(
                        new PostTranslation(this, locale, series, title, excerpt, body))
        );
    }

    public void removeTranslation(LocaleCode locale) {
        this.translations.removeIf(t -> t.getLocale() == locale);
    }

    public Optional<PostTranslation> findTranslation(LocaleCode locale) {
        return translations.stream().filter(t -> t.getLocale() == locale).findFirst();
    }

    public boolean hasTranslation(LocaleCode locale) {
        return findTranslation(locale).isPresent();
    }
}
