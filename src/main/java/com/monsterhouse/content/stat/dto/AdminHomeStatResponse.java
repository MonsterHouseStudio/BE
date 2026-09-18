package com.monsterhouse.content.stat.dto;

import com.monsterhouse.content.stat.entity.HomeStat;

public record AdminHomeStatResponse(
        Long id,
        Integer valueNumber,
        String suffix,
        String valueText,
        String labelKo,
        String labelJa,
        String descKo,
        String descJa,
        String photoKey,
        String photoUrl,
        boolean active,
        int sortOrder
) {
    public static AdminHomeStatResponse of(HomeStat s, String photoUrl) {
        return new AdminHomeStatResponse(
                s.getId(),
                s.getValueNumber(),
                s.getSuffix(),
                s.getValueText(),
                s.getLabelKo(),
                s.getLabelJa(),
                s.getDescKo(),
                s.getDescJa(),
                s.getPhotoKey(),
                photoUrl,
                s.isActive(),
                s.getSortOrder()
        );
    }
}
