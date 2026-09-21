package com.monsterhouse.content.about.controller;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.content.about.dto.AboutPageResponse;
import com.monsterhouse.content.about.service.AboutIntroService;
import com.monsterhouse.content.about.service.AboutVideoService;
import com.monsterhouse.content.about.service.CrewService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 소개 페이지 공개 API. 인트로 + 크루 + 최신 영상을 한 번에 반환. */
@RestController
@RequestMapping("/api/about")
@RequiredArgsConstructor
public class AboutController {

    private final AboutIntroService introService;
    private final CrewService crewService;
    private final AboutVideoService videoService;

    @GetMapping
    public ApiResponse<AboutPageResponse> get() {
        LocaleCode locale = LocaleCode.from(LocaleContextHolder.getLocale());
        return ApiResponse.ok(new AboutPageResponse(
                introService.findPublic(locale),
                crewService.findActive(locale),
                videoService.findActive(locale)
        ));
    }
}
