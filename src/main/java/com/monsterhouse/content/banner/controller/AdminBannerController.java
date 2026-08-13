package com.monsterhouse.content.banner.controller;

import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.content.banner.dto.AdminBannerResponse;
import com.monsterhouse.content.banner.dto.BannerSaveRequest;
import com.monsterhouse.content.banner.service.BannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class AdminBannerController {
    private final BannerService bannerService;
    @GetMapping
    public ApiResponse<List<AdminBannerResponse>> list(){
        return ApiResponse.ok(bannerService.findAllForAdmin());
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminBannerResponse>create(@Valid @RequestBody BannerSaveRequest request){
        return ApiResponse.ok(bannerService.create(request));
    }
    @PutMapping("/{id}")
    public ApiResponse<AdminBannerResponse> update(@PathVariable Long id,@Valid @RequestBody BannerSaveRequest request) {
        return ApiResponse.ok(bannerService.update(id, request));
    }
    @PatchMapping("/{id}/active")
    public ApiResponse<AdminBannerResponse> setActive(@PathVariable Long id, @RequestParam boolean active){
        return ApiResponse.ok(bannerService.setActive(id, active));
    }
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id){
        bannerService.delete(id);
        return ApiResponse.ok();
    }
}
