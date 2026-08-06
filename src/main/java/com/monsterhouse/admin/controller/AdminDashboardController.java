package com.monsterhouse.admin.controller;

import com.monsterhouse.admin.dto.DashboardSummaryResponse;
import com.monsterhouse.admin.service.AdminDashboardService;
import com.monsterhouse.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final AdminDashboardService dashboardService;
    @GetMapping
    public ApiResponse<DashboardSummaryResponse>summary(){
        return ApiResponse.ok(dashboardService.summary());
    }
}
