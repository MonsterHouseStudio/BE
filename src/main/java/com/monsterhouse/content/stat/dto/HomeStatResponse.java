package com.monsterhouse.content.stat.dto;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.content.stat.entity.HomeStat;

/**
 * 공개용. valueNumber 가 있으면 프론트가 카운트업 후 suffix 를 붙이고,
 * 없으면 valueText 를 그대로 표시합니다.
 */
public record HomeStatResponse(
        Long id,
        Integer valueNumber,
        String suffix,
        String valueText,
        String label,
        String description,
        String photoUrl
) {
    public static HomeStatResponse of(HomeStat s, LocaleCode locale, String photoUrl) {
        return new HomeStatResponse(
                s.getId(),
                s.getValueNumber(),
                s.getSuffix(),
                s.getValueText(),
                s.label(locale),
                s.desc(locale),
                photoUrl
        );
    }
}
