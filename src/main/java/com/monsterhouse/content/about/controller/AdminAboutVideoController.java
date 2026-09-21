package com.monsterhouse.content.about.controller;

import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.content.about.dto.AboutVideoSaveRequest;
import com.monsterhouse.content.about.dto.AdminAboutVideoResponse;
import com.monsterhouse.content.about.service.AboutVideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/about-videos")
@RequiredArgsConstructor
public class AdminAboutVideoController {
    private final AboutVideoService service;

    @GetMapping
    public ApiResponse<List<AdminAboutVideoResponse>> list() {
        return ApiResponse.ok(service.findAllForAdmin());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminAboutVideoResponse> create(@Valid @RequestBody AboutVideoSaveRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminAboutVideoResponse> update(@PathVariable Long id, @Valid @RequestBody AboutVideoSaveRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<AdminAboutVideoResponse> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return ApiResponse.ok(service.setActive(id, active));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok();
    }
}
