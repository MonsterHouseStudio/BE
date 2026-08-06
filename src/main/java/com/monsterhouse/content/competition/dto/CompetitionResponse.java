package com.monsterhouse.content.competition.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.content.competition.entity.Competition;
import com.monsterhouse.content.competition.entity.CompetitionTranslation;
import com.monsterhouse.content.competition.entity.Country;

import java.time.LocalDate;

/** 프론트 SchedulePage 가 쓰는 형태와 1:1 로 맞췄습니다. */
public record CompetitionResponse(
        Long id,
        Country country,
        String name,
        String description,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,

        String place,
        String host,
        String link
) {

    public static CompetitionResponse of(Competition competition, LocaleCode locale) {
        // findPublished 로 가져온 경우 해당 locale 번역이 반드시 존재합니다.
        CompetitionTranslation t = competition.findTranslation(locale)
                .orElseThrow(() -> new IllegalStateException(
                        "번역이 없는 대회가 공개 목록에 들어왔습니다. id=" + competition.getId()));

        return new CompetitionResponse(
                competition.getId(),
                competition.getCountry(),
                t.getName(),
                t.getDescription(),
                competition.getStartDate(),
                competition.getEndDate(),
                t.getPlace(),
                t.getHost(),
                competition.getLink()
        );
    }
}
