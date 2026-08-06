package com.monsterhouse.content.post.entity;

import com.monsterhouse.common.enums.LocaleCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "post_translation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_translation",
                columnNames = {"post_id", "locale"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_post_translation_post"))
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(name = "locale", nullable = false, length = 5)
    private LocaleCode locale;

    /**
     * 시리즈명도 번역 대상입니다.
     * "준영의 첫 시합" / "ジュニョンの初大会" 처럼 언어마다 표기가 다릅니다.
     */
    @Column(name = "series", length = 100)
    private String series;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** 목록 카드에 쓰는 요약. 본문에서 자동 추출하지 않고 직접 씁니다. */
    @Column(name = "excerpt", length = 500)
    private String excerpt;

    @Column(name = "body", nullable = false, columnDefinition = "LONGTEXT")
    private String body;

    PostTranslation(Post post, LocaleCode locale, String series,
                    String title, String excerpt, String body) {
        this.post = post;
        this.locale = locale;
        this.series = series;
        this.title = title;
        this.excerpt = excerpt;
        this.body = body;
    }

    void update(String series, String title, String excerpt, String body) {
        this.series = series;
        this.title = title;
        this.excerpt = excerpt;
        this.body = body;
    }
}
