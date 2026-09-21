package com.monsterhouse.content.about.entity;

import com.monsterhouse.common.entity.BaseTimeEntity;
import com.monsterhouse.common.enums.LocaleCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 소개 페이지 크루 구성원. 이름·역할·소개(한/일) + 사진. */
@Getter
@Entity
@Table(name = "crew")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Crew extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_ko", length = 60)
    private String nameKo;
    @Column(name = "name_ja", length = 60)
    private String nameJa;

    @Column(name = "role_ko", length = 60)
    private String roleKo;
    @Column(name = "role_ja", length = 60)
    private String roleJa;

    @Column(name = "bio_ko", length = 500)
    private String bioKo;
    @Column(name = "bio_ja", length = 500)
    private String bioJa;

    @Column(name = "photo_key", length = 300)
    private String photoKey;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder
    private Crew(String nameKo, String nameJa, String roleKo, String roleJa,
                String bioKo, String bioJa, String photoKey, boolean active, int sortOrder) {
        this.nameKo = nameKo;
        this.nameJa = nameJa;
        this.roleKo = roleKo;
        this.roleJa = roleJa;
        this.bioKo = bioKo;
        this.bioJa = bioJa;
        this.photoKey = photoKey;
        this.active = active;
        this.sortOrder = sortOrder;
    }

    public void update(String nameKo, String nameJa, String roleKo, String roleJa,
                       String bioKo, String bioJa, String photoKey, int sortOrder) {
        this.nameKo = nameKo;
        this.nameJa = nameJa;
        this.roleKo = roleKo;
        this.roleJa = roleJa;
        this.bioKo = bioKo;
        this.bioJa = bioJa;
        this.photoKey = photoKey;
        this.sortOrder = sortOrder;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    private String pick(LocaleCode locale, String ko, String ja) {
        if (locale == LocaleCode.JA && ja != null && !ja.isBlank()) return ja;
        return ko;
    }

    public String name(LocaleCode locale) { return pick(locale, nameKo, nameJa); }
    public String role(LocaleCode locale) { return pick(locale, roleKo, roleJa); }
    public String bio(LocaleCode locale) { return pick(locale, bioKo, bioJa); }
}
