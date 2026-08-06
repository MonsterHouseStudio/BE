package com.monsterhouse.content.post.controller;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.common.response.PageResponse;
import com.monsterhouse.content.post.dto.PostDetailResponse;
import com.monsterhouse.content.post.dto.PostSummaryResponse;
import com.monsterhouse.content.post.entity.PostCategory;
import com.monsterhouse.content.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public ApiResponse<PageResponse<PostSummaryResponse>> list(
            @RequestParam(required = false) PostCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        LocaleCode locale = LocaleCode.from(LocaleContextHolder.getLocale());
        return ApiResponse.ok(PageResponse.of(
                postService.findPublished(locale, category, PageRequest.of(page, size))));
    }

    @GetMapping("/{slug}")
    public ApiResponse<PostDetailResponse> detail(@PathVariable String slug) {
        LocaleCode locale = LocaleCode.from(LocaleContextHolder.getLocale());
        return ApiResponse.ok(postService.findBySlug(slug, locale));
    }
}
