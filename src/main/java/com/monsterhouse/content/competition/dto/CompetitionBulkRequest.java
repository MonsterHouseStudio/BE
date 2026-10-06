package com.monsterhouse.content.competition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 시합 일정 엑셀 일괄 등록 요청. */
public record CompetitionBulkRequest(
        @NotEmpty
        @Size(max = 2000)
        List<@Valid CompetitionSaveRequest> items
) {
}
