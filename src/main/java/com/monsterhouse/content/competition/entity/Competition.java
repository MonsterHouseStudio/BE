package com.monsterhouse.content.competition.entity;

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
 * 시합 일정 (기획서 §6.1).
 *
 * 언어와 무관한 값(국가·날짜·링크)은 여기에, 언어별 값(대회명·설명·장소·주최)은
 * CompetitionTranslation 에 둡니다.
 *
 * 장소를 번역 테이블에 넣은 이유:
 *   기획서 초안은 place_ko / place_ja 컬럼이었지만, 그러면 언어가 하나 늘 때마다
 *   컬럼이 늘어납니다. 번역 테이블이면 행만 늘면 됩니다.
 */
@Getter
@Entity
@Table(
        name = "competition",
        indexes = {
                @Index(name = "idx_competition_start", columnList = "start_date"),
                @Index(name = "idx_competition_country", columnList = "country")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Competition extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "country", nullable = false, length = 2)
    private Country country;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** 하루짜리 대회면 startDate 와 같은 값 */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "link", length = 500)
    private String link;

    @Column(name = "published", nullable = false)
    private boolean published;

    @OneToMany(mappedBy = "competition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompetitionTranslation> translations = new ArrayList<>();

    @Builder
    private Competition(Country country, LocalDate startDate, LocalDate endDate,
                        String link, boolean published) {
        this.country = country;
        this.startDate = startDate;
        this.endDate = endDate;
        this.link = link;
        this.published = published;
    }

    public void update(Country country, LocalDate startDate, LocalDate endDate,
                       String link, boolean published) {
        this.country = country;
        this.startDate = startDate;
        this.endDate = endDate;
        this.link = link;
        this.published = published;
    }

    /** 해당 언어 번역이 있으면 갈아끼우고, 없으면 새로 답니다. */
    public void putTranslation(LocaleCode locale, String name, String description,
                               String place, String host) {
        findTranslation(locale).ifPresentOrElse(
                t -> t.update(name, description, place, host),
                () -> this.translations.add(
                        new CompetitionTranslation(this, locale, name, description, place, host))
        );
    }

    public void removeTranslation(LocaleCode locale) {
        this.translations.removeIf(t -> t.getLocale() == locale);
    }

    public Optional<CompetitionTranslation> findTranslation(LocaleCode locale) {
        return translations.stream().filter(t -> t.getLocale() == locale).findFirst();
    }

    public boolean hasTranslation(LocaleCode locale) {
        return findTranslation(locale).isPresent();
    }
}
