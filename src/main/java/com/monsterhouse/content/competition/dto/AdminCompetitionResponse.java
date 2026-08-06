package com.monsterhouse.content.competition.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.content.competition.entity.Competition;
import com.monsterhouse.content.competition.entity.Country;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 관리자용 — 모든 언어의 번역을 함께 내려주고,
 * 빠진 언어를 missingLocales 로 알려 "일본어 미작성" 배지를 띄울 수 있게 합니다(기획서 §3.2).
 */
public record AdminCompetitionResponse(
        Long id,
        Country country,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,

        String link,
        boolean published,
        List<CompetitionSaveRequest.Translation> translations,
        List<LocaleCode> missingLocales
) {

    public static AdminCompetitionResponse of(Competition competition) {
        List<CompetitionSaveRequest.Translation> translations = competition.getTranslations().stream()
                .map(t -> new CompetitionSaveRequest.Translation(
                        t.getLocale(), t.getName(), t.getDescription(), t.getPlace(), t.getHost()))
                .toList();

        List<LocaleCode> missing = Arrays.stream(LocaleCode.values())
                .filter(locale -> !competition.hasTranslation(locale))
                .toList();

        return new AdminCompetitionResponse(
                competition.getId(),
                competition.getCountry(),
                competition.getStartDate(),
                competition.getEndDate(),
                competition.getLink(),
                competition.isPublished(),
                translations,
                missing
        );
    }
}
