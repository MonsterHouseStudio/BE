package com.monsterhouse.content.gallery.controller;

import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.content.gallery.dto.AdminGalleryItemResponse;
import com.monsterhouse.content.gallery.dto.GallerySaveRequest;
import com.monsterhouse.content.gallery.service.GalleryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/gallery")
@RequiredArgsConstructor
public class AdminGalleryController {

    private final GalleryService galleryService;

    @GetMapping
    public ApiResponse<List<AdminGalleryItemResponse>> list() {
        return ApiResponse.ok(galleryService.findAllForAdmin());
    }

    @GetMapping("/awaiting-consent-count")
    public ApiResponse<Long> awaitingConsentCount() {
        return ApiResponse.ok(galleryService.countAwaitingConsent());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminGalleryItemResponse> create(
            @Valid @RequestBody GallerySaveRequest request) {
        return ApiResponse.ok(galleryService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminGalleryItemResponse> update(
            @PathVariable Long id, @Valid @RequestBody GallerySaveRequest request) {
        return ApiResponse.ok(galleryService.update(id, request));
    }

    /** 게시 동의 토글 — 삭제 요청 시 즉시 비공개로 돌리는 용도 (기획서 §9) */
    @PatchMapping("/{id}/consent")
    public ApiResponse<AdminGalleryItemResponse> changeConsent(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {

        boolean consent = Boolean.TRUE.equals(body.get("consent"));
        String note = body.get("note") == null ? null : String.valueOf(body.get("note"));

        return ApiResponse.ok(galleryService.changeConsent(id, consent, note));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        galleryService.delete(id);
        return ApiResponse.ok();
    }
}
