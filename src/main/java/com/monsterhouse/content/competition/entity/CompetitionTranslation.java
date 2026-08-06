package com.monsterhouse.content.competition.entity;

import com.monsterhouse.common.enums.LocaleCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "competition_translation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_competition_translation",
                columnNames = {"competition_id", "locale"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompetitionTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_competition_translation_competition"))
    private Competition competition;

    @Enumerated(EnumType.STRING)
    @Column(name = "locale", nullable = false, length = 5)
    private LocaleCode locale;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "place", length = 200)
    private String place;

    @Column(name = "host", length = 200)
    private String host;

    CompetitionTranslation(Competition competition, LocaleCode locale,
                           String name, String description, String place, String host) {
        this.competition = competition;
        this.locale = locale;
        this.name = name;
        this.description = description;
        this.place = place;
        this.host = host;
    }

    void update(String name, String description, String place, String host) {
        this.name = name;
        this.description = description;
        this.place = place;
        this.host = host;
    }
}
