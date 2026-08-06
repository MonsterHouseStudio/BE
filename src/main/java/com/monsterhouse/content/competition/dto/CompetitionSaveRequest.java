package com.monsterhouse.content.competition.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.content.competition.entity.Country;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 관리자 저장 요청.
 *
 * translations 를 리스트로 받는 이유:
 *   언어가 늘어도 DTO 를 고칠 필요가 없고, "일본어만 나중에 추가" 같은
 *   언어별 독립 발행(기획서 §3.2)이 자연스럽게 됩니다.
 */
public record CompetitionSaveRequest(

        @NotNull
        Country country,

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,

        @Size(max = 500)
        String link,

        boolean published,

        /** 최소 1개는 있어야 합니다. 전부 비면 어느 언어에서도 보이지 않는 유령 데이터가 됩니다. */
        @NotEmpty
        @Valid
        List<Translation> translations
) {

    public record Translation(

            @NotNull
            LocaleCode locale,

            @NotBlank
            @Size(max = 200)
            String name,

            String description,

            @Size(max = 200)
            String place,

            @Size(max = 200)
            String host
    ) {
    }
}
