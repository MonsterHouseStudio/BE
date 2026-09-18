package com.monsterhouse.content.stat.controller;

import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.content.stat.dto.AdminHomeStatResponse;
import com.monsterhouse.content.stat.dto.HomeStatSaveRequest;
import com.monsterhouse.content.stat.service.HomeStatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/home-stats")
@RequiredArgsConstructor
public class AdminHomeStatController {
    private final HomeStatService service;

    @GetMapping
    public ApiResponse<List<AdminHomeStatResponse>> list() {
        return ApiResponse.ok(service.findAllForAdmin());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminHomeStatResponse> create(@Valid @RequestBody HomeStatSaveRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminHomeStatResponse> update(@PathVariable Long id, @Valid @RequestBody HomeStatSaveRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<AdminHomeStatResponse> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return ApiResponse.ok(service.setActive(id, active));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok();
    }
}
