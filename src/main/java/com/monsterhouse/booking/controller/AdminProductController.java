package com.monsterhouse.booking.controller;

import com.monsterhouse.booking.dto.request.ProductSaveRequest;
import com.monsterhouse.booking.dto.response.AdminProductResponse;
import com.monsterhouse.booking.service.AdminProductService;
import com.monsterhouse.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService productService;

    @GetMapping
    public ApiResponse<List<AdminProductResponse>> list() {
        return ApiResponse.ok(productService.findAll());
    }

    @GetMapping("/{productId}")
    public ApiResponse<AdminProductResponse> detail(@PathVariable Long productId) {
        return ApiResponse.ok(productService.findOne(productId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminProductResponse> create(@Valid @RequestBody ProductSaveRequest request) {
        return ApiResponse.ok(productService.create(request));
    }

    @PutMapping("/{productId}")
    public ApiResponse<AdminProductResponse> update(@PathVariable Long productId,
                                                    @Valid @RequestBody ProductSaveRequest request) {
        return ApiResponse.ok(productService.update(productId, request));
    }

    @PatchMapping("/{productId}/active")
    public ApiResponse<AdminProductResponse> setActive(@PathVariable Long productId,
                                                       @RequestBody Map<String, Boolean> body) {
        return ApiResponse.ok(productService.setActive(productId, Boolean.TRUE.equals(body.get("active"))));
    }

    /** 되돌릴 수 없는 작업이라 SUPER_ADMIN 만 허용합니다. */
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long productId) {
        productService.delete(productId);
        return ApiResponse.ok();
    }
}