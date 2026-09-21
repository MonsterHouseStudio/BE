package com.monsterhouse.content.about.dto;

import com.monsterhouse.content.about.entity.Crew;

public record AdminCrewResponse(
        Long id,
        String nameKo,
        String nameJa,
        String roleKo,
        String roleJa,
        String bioKo,
        String bioJa,
        String photoKey,
        String photoUrl,
        boolean active,
        int sortOrder
) {
    public static AdminCrewResponse of(Crew c, String photoUrl) {
        return new AdminCrewResponse(
                c.getId(),
                c.getNameKo(), c.getNameJa(),
                c.getRoleKo(), c.getRoleJa(),
                c.getBioKo(), c.getBioJa(),
                c.getPhotoKey(), photoUrl,
                c.isActive(), c.getSortOrder()
        );
    }
}
