package com.monsterhouse.admin.controller;
import com.monsterhouse.admin.dto.AdminProfileResponse;
import com.monsterhouse.admin.dto.AdminUserCreateRequest;
import com.monsterhouse.admin.dto.PasswordChangeRequest;
import com.monsterhouse.admin.security.AdminPrincipal;
import com.monsterhouse.admin.service.AdminUserService;
import com.monsterhouse.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final AdminUserService adminUserService;
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<List<AdminProfileResponse>> list(){
        return ApiResponse.ok(adminUserService.findAll());
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<AdminProfileResponse> create(
            @Valid @RequestBody AdminUserCreateRequest request){
        return ApiResponse.ok(adminUserService.create(request));
    }
    @PostMapping("/{adminUserId}/disable")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> disable(@PathVariable Long adminUserId, @AuthenticationPrincipal AdminPrincipal principal){
        adminUserService.disable(adminUserId, principal.id());
        return ApiResponse.ok();
    }
    @PostMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal AdminPrincipal principal, @Valid @RequestBody PasswordChangeRequest request){
        adminUserService.changePassword(principal.id(),request);
        return ApiResponse.ok();
    }
}
