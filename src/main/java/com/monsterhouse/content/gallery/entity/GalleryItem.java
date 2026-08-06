package com.monsterhouse.content.gallery.entity;

import com.monsterhouse.booking.entity.ProductType;
import com.monsterhouse.common.entity.BaseTimeEntity;
import com.monsterhouse.common.enums.LocaleCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 갤러리 사진 (기획서 §6.2).
 *
 * ★ consent (초상권 게시 동의)
 *   인물이 식별되는 사진은 동의를 받은 경우에만 공개합니다.
 *   기본값을 false 로 둔 것이 중요합니다. 실수로 올린 사진이 곧바로 공개되면
 *   되돌릴 수 없는 문제가 됩니다. "동의를 받았다"는 명시적 행위로만 공개됩니다.
 *
 * 카테고리는 촬영 상품과 같은 분류를 씁니다(바디프로필/모티베이션/센터홍보).
 * 별도 enum 을 만들면 상품이 늘 때마다 두 곳을 고쳐야 합니다.
 */
@Getter
@Entity
@Table(
        name = "gallery_item",
        indexes = {
                @Index(name = "idx_gallery_consent_sort", columnList = "consent, sort_order"),
                @Index(name = "idx_gallery_category", columnList = "category")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GalleryItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private ProductType category;

    @Column(name = "image_key", nullable = false, length = 300)
    private String imageKey;

    @Column(name = "thumb_key", nullable = false, length = 300)
    private String thumbKey;

    /** portrait | landscape | square — 업로드 시 실제 크기로 판정합니다. */
    @Column(name = "ratio", nullable = false, length = 20)
    private String ratio;

    @Column(name = "taken_at")
    private LocalDate takenAt;

    /** ★ 게시 동의. 기본 false — 동의 없이는 공개되지 않습니다. */
    @Column(name = "consent", nullable = false)
    private boolean consent;

    /** 동의를 누구에게 어떤 형태로 받았는지 기록. 분쟁 시 근거가 됩니다. */
    @Column(name = "consent_note", length = 300)
    private String consentNote;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "galleryItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GalleryTranslation> translations = new ArrayList<>();

    @Builder
    private GalleryItem(ProductType category, String imageKey, String thumbKey, String ratio,
                        LocalDate takenAt, boolean consent, String consentNote, int sortOrder) {
        this.category = category;
        this.imageKey = imageKey;
        this.thumbKey = thumbKey;
        this.ratio = ratio;
        this.takenAt = takenAt;
        this.consent = consent;
        this.consentNote = consentNote;
        this.sortOrder = sortOrder;
    }

    public void update(ProductType category, LocalDate takenAt, int sortOrder) {
        this.category = category;
        this.takenAt = takenAt;
        this.sortOrder = sortOrder;
    }

    /**
     * 동의 상태 변경. 삭제 요청이 들어오면 false 로 되돌려 즉시 비공개 처리합니다
     * (기획서 §9 — 삭제 요청 처리 절차).
     */
    public void changeConsent(boolean consent, String consentNote) {
        this.consent = consent;
        this.consentNote = consentNote;
    }

    public void replaceImage(String imageKey, String thumbKey, String ratio) {
        this.imageKey = imageKey;
        this.thumbKey = thumbKey;
        this.ratio = ratio;
    }

    public void putTranslation(LocaleCode locale, String caption) {
        findTranslation(locale).ifPresentOrElse(
                t -> t.update(caption),
                () -> this.translations.add(new GalleryTranslation(this, locale, caption))
        );
    }

    public void removeTranslation(LocaleCode locale) {
        this.translations.removeIf(t -> t.getLocale() == locale);
    }

    public Optional<GalleryTranslation> findTranslation(LocaleCode locale) {
        return translations.stream().filter(t -> t.getLocale() == locale).findFirst();
    }

    /**
     * 캡션은 콘텐츠 글과 달리 폴백을 허용합니다.
     * 캡션이 없다고 사진 자체를 숨기면 갤러리가 텅 비어 보입니다.
     */
    public String caption(LocaleCode locale) {
        return findTranslation(locale)
                .map(GalleryTranslation::getCaption)
                .filter(c -> c != null && !c.isBlank())
                .orElseGet(() -> findTranslation(LocaleCode.DEFAULT)
                        .map(GalleryTranslation::getCaption)
                        .orElse(null));
    }
}
