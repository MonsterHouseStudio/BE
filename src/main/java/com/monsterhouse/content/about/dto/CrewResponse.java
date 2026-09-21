package com.monsterhouse.content.about.dto;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.content.about.entity.Crew;

public record CrewResponse(
        Long id,
        String name,
        String role,
        String bio,
        String photoUrl
) {
    public static CrewResponse of(Crew c, LocaleCode locale, String photoUrl) {
        return new CrewResponse(
                c.getId(),
                c.name(locale),
                c.role(locale),
                c.bio(locale),
                photoUrl
        );
    }
}
