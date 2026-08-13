package com.monsterhouse.content.banner.controller;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.content.banner.dto.BannerResponse;
import com.monsterhouse.content.banner.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannerController {
    private final BannerService bannerService;
    @GetMapping
    public ApiResponse<List<BannerResponse>> list(){
        LocaleCode locale = LocaleCode.from(LocaleContextHolder.getLocale());
        return ApiResponse.ok(bannerService.findActive(locale));
    }
}
