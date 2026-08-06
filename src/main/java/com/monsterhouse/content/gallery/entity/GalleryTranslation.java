package com.monsterhouse.content.gallery.entity;

import com.monsterhouse.common.enums.LocaleCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "gallery_translation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_gallery_translation",
                columnNames = {"gallery_item_id", "locale"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GalleryTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gallery_item_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_gallery_translation_item"))
    private GalleryItem galleryItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "locale", nullable = false, length = 5)
    private LocaleCode locale;

    @Column(name = "caption", length = 300)
    private String caption;

    GalleryTranslation(GalleryItem galleryItem, LocaleCode locale, String caption) {
        this.galleryItem = galleryItem;
        this.locale = locale;
        this.caption = caption;
    }

    void update(String caption) {
        this.caption = caption;
    }
}
