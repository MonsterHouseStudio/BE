package com.monsterhouse.content.about.dto;

import com.monsterhouse.content.about.entity.AboutIntro;

public record AdminAboutIntroResponse(
        String titleKo,
        String titleJa,
        String descKo,
        String descJa,
        String photo1Key,
        String photo1Url,
        String photo2Key,
        String photo2Url,
        String photo3Key,
        String photo3Url
) {
    public static AdminAboutIntroResponse of(AboutIntro a, String url1, String url2, String url3) {
        return new AdminAboutIntroResponse(
                a.getTitleKo(), a.getTitleJa(),
                a.getDescKo(), a.getDescJa(),
                a.getPhoto1Key(), url1,
                a.getPhoto2Key(), url2,
                a.getPhoto3Key(), url3
        );
    }
}
