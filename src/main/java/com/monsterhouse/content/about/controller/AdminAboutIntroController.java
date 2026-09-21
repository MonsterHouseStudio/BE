package com.monsterhouse.content.about.controller;

import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.content.about.dto.AboutIntroSaveRequest;
import com.monsterhouse.content.about.dto.AdminAboutIntroResponse;
import com.monsterhouse.content.about.service.AboutIntroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/about-intro")
@RequiredArgsConstructor
public class AdminAboutIntroController {
    private final AboutIntroService service;

    @GetMapping
    public ApiResponse<AdminAboutIntroResponse> get() {
        return ApiResponse.ok(service.findForAdmin());
    }

    @PutMapping
    public ApiResponse<AdminAboutIntroResponse> save(@Valid @RequestBody AboutIntroSaveRequest request) {
        return ApiResponse.ok(service.save(request));
    }
}
