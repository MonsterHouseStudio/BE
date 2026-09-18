package com.monsterhouse.content.stat.entity;

import com.monsterhouse.common.entity.BaseTimeEntity;
import com.monsterhouse.common.enums.LocaleCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 홈 "숫자로 보는" 풀스크린 스크롤 패널 한 칸.
 *
 * 큰 값(숫자 카운트업 또는 텍스트) + 라벨 + 설명문 + 배경 사진으로 구성됩니다.
 * - valueNumber 가 있으면: 프론트에서 그 수까지 카운트업 후 suffix 를 붙입니다(예: 480 + "+").
 * - valueNumber 가 없으면: valueText 를 그대로 보여줍니다(예: "KR · JP").
 */
@Getter
@Entity
@Table(name = "home_stat")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HomeStat extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 카운트업 목표 숫자. 비숫자 값(KR · JP 등)이면 null. */
    @Column(name = "value_number")
    private Integer valueNumber;

    /** 숫자 뒤에 붙는 기호(예: "+"). 없으면 빈 문자열. */
    @Column(name = "suffix", length = 10)
    private String suffix;

    /** valueNumber 가 null 일 때 그대로 표시할 텍스트(예: "KR · JP"). */
    @Column(name = "value_text", length = 40)
    private String valueText;

    /** 큰 값 밑 라벨(예: 누적 촬영). */
    @Column(name = "label_ko", length = 60)
    private String labelKo;
    @Column(name = "label_ja", length = 60)
    private String labelJa;

    /** 라벨 밑 작은 설명문. */
    @Column(name = "desc_ko", length = 300)
    private String descKo;
    @Column(name = "desc_ja", length = 300)
    private String descJa;

    /** 배경 사진 스토리지 키. 없으면 프론트가 브랜드 그라디언트로 대체. */
    @Column(name = "photo_key", length = 300)
    private String photoKey;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder
    private HomeStat(Integer valueNumber, String suffix, String valueText,
                     String labelKo, String labelJa, String descKo, String descJa,
                     String photoKey, boolean active, int sortOrder) {
        this.valueNumber = valueNumber;
        this.suffix = suffix == null ? "" : suffix;
        this.valueText = valueText;
        this.labelKo = labelKo;
        this.labelJa = labelJa;
        this.descKo = descKo;
        this.descJa = descJa;
        this.photoKey = photoKey;
        this.active = active;
        this.sortOrder = sortOrder;
    }

    public void update(Integer valueNumber, String suffix, String valueText,
                       String labelKo, String labelJa, String descKo, String descJa,
                       String photoKey, int sortOrder) {
        this.valueNumber = valueNumber;
        this.suffix = suffix == null ? "" : suffix;
        this.valueText = valueText;
        this.labelKo = labelKo;
        this.labelJa = labelJa;
        this.descKo = descKo;
        this.descJa = descJa;
        this.photoKey = photoKey;
        this.sortOrder = sortOrder;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String label(LocaleCode locale) {
        if (locale == LocaleCode.JA && labelJa != null && !labelJa.isBlank()) {
            return labelJa;
        }
        return labelKo;
    }

    public String desc(LocaleCode locale) {
        if (locale == LocaleCode.JA && descJa != null && !descJa.isBlank()) {
            return descJa;
        }
        return descKo;
    }
}
