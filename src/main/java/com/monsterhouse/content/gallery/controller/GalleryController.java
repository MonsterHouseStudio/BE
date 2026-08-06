package com.monsterhouse.content.gallery.controller;

import com.monsterhouse.booking.entity.ProductType;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.content.gallery.dto.GalleryItemResponse;
import com.monsterhouse.content.gallery.service.GalleryService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gallery")
@RequiredArgsConstructor
public class GalleryController {

    private final GalleryService galleryService;

    @GetMapping
    public ApiResponse<List<GalleryItemResponse>> list(
            @RequestParam(required = false) ProductType category) {

        LocaleCode locale = LocaleCode.from(LocaleContextHolder.getLocale());
        return ApiResponse.ok(galleryService.findPublic(category, locale));
    }
}
