package com.monsterhouse.content.about.controller;

import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.content.about.dto.AdminCrewResponse;
import com.monsterhouse.content.about.dto.CrewSaveRequest;
import com.monsterhouse.content.about.service.CrewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/crew")
@RequiredArgsConstructor
public class AdminCrewController {
    private final CrewService service;

    @GetMapping
    public ApiResponse<List<AdminCrewResponse>> list() {
        return ApiResponse.ok(service.findAllForAdmin());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminCrewResponse> create(@Valid @RequestBody CrewSaveRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminCrewResponse> update(@PathVariable Long id, @Valid @RequestBody CrewSaveRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<AdminCrewResponse> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return ApiResponse.ok(service.setActive(id, active));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok();
    }
}
