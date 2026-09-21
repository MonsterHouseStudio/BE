package com.monsterhouse.content.about.entity;

import com.monsterhouse.common.entity.BaseTimeEntity;
import com.monsterhouse.common.enums.LocaleCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 소개 페이지 메인 배너(인트로). 항상 한 행만 존재하는 싱글턴(id=1).
 * 제목·설명(한/일) + 콜라주 사진 3장.
 */
@Getter
@Entity
@Table(name = "about_intro")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AboutIntro extends BaseTimeEntity {

    /** 싱글턴 고정 식별자. */
    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "title_ko", length = 200)
    private String titleKo;
    @Column(name = "title_ja", length = 200)
    private String titleJa;

    @Column(name = "desc_ko", length = 1000)
    private String descKo;
    @Column(name = "desc_ja", length = 1000)
    private String descJa;

    @Column(name = "photo1_key", length = 300)
    private String photo1Key;
    @Column(name = "photo2_key", length = 300)
    private String photo2Key;
    @Column(name = "photo3_key", length = 300)
    private String photo3Key;

    public AboutIntro(String titleKo, String titleJa, String descKo, String descJa,
                      String photo1Key, String photo2Key, String photo3Key) {
        this.id = SINGLETON_ID;
        this.titleKo = titleKo;
        this.titleJa = titleJa;
        this.descKo = descKo;
        this.descJa = descJa;
        this.photo1Key = photo1Key;
        this.photo2Key = photo2Key;
        this.photo3Key = photo3Key;
    }

    public void update(String titleKo, String titleJa, String descKo, String descJa,
                       String photo1Key, String photo2Key, String photo3Key) {
        this.titleKo = titleKo;
        this.titleJa = titleJa;
        this.descKo = descKo;
        this.descJa = descJa;
        this.photo1Key = photo1Key;
        this.photo2Key = photo2Key;
        this.photo3Key = photo3Key;
    }

    private String pick(LocaleCode locale, String ko, String ja) {
        if (locale == LocaleCode.JA && ja != null && !ja.isBlank()) return ja;
        return ko;
    }

    public String title(LocaleCode locale) { return pick(locale, titleKo, titleJa); }
    public String desc(LocaleCode locale) { return pick(locale, descKo, descJa); }
}
