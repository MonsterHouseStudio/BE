package com.monsterhouse.content.competition.controller;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.content.competition.dto.CompetitionResponse;
import com.monsterhouse.content.competition.service.CompetitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/competitions")
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionService competitionService;

    /**
     * 지난 시합/예정 구분과 국가 필터는 프론트에서 처리합니다.
     * 건수가 수십 건 규모라 전부 내려주고 클라이언트에서 거르는 편이
     * 탭 전환마다 요청하는 것보다 빠릅니다.
     */
    @GetMapping
    public ApiResponse<List<CompetitionResponse>> list() {
        LocaleCode locale = LocaleCode.from(LocaleContextHolder.getLocale());
        return ApiResponse.ok(competitionService.findPublished(locale));
    }
}
