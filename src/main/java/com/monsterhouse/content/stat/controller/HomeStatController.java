package com.monsterhouse.content.stat.controller;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.content.stat.dto.HomeStatResponse;
import com.monsterhouse.content.stat.service.HomeStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/home-stats")
@RequiredArgsConstructor
public class HomeStatController {
    private final HomeStatService service;

    @GetMapping
    public ApiResponse<List<HomeStatResponse>> list() {
        LocaleCode locale = LocaleCode.from(LocaleContextHolder.getLocale());
        return ApiResponse.ok(service.findActive(locale));
    }
}
