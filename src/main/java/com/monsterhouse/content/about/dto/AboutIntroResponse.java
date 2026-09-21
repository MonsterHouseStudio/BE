package com.monsterhouse.content.about.dto;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.content.about.entity.AboutIntro;

public record AboutIntroResponse(
        String title,
        String description,
        String photo1Url,
        String photo2Url,
        String photo3Url
) {
    public static AboutIntroResponse of(AboutIntro a, LocaleCode locale,
                                        String url1, String url2, String url3) {
        return new AboutIntroResponse(
                a.title(locale),
                a.desc(locale),
                url1, url2, url3
        );
    }
}
