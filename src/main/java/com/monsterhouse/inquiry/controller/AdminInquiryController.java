package com.monsterhouse.inquiry.controller;

import com.monsterhouse.admin.security.AdminPrincipal;
import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.common.response.PageResponse;
import com.monsterhouse.inquiry.dto.response.AdminInquiryResponse;
import com.monsterhouse.inquiry.entity.InquiryStatus;
import com.monsterhouse.inquiry.entity.InquiryType;
import com.monsterhouse.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final InquiryService inquiryService;

    @GetMapping
    public ApiResponse<PageResponse<AdminInquiryResponse>> list(
            @RequestParam(required = false) InquiryType type,
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.ok(PageResponse.of(
                inquiryService.search(type, status, PageRequest.of(page, size))));
    }

    @GetMapping("/pending-count")
    public ApiResponse<Long> pendingCount() {
        return ApiResponse.ok(inquiryService.countPending());
    }

    /** 처리 완료 표시. 누가 처리했는지 토큰에서 꺼내 기록합니다. */
    @PostMapping("/{inquiryId}/handle")
    public ApiResponse<AdminInquiryResponse> handle(
            @PathVariable Long inquiryId,
            @AuthenticationPrincipal AdminPrincipal principal,
            @RequestBody(required = false) Map<String, String> body) {

        String memo = body == null ? null : body.get("memo");
        return ApiResponse.ok(inquiryService.markHandled(inquiryId, principal.username(), memo));
    }

    @PostMapping("/{inquiryId}/pending")
    public ApiResponse<AdminInquiryResponse> pending(@PathVariable Long inquiryId) {
        return ApiResponse.ok(inquiryService.markPending(inquiryId));
    }
}
