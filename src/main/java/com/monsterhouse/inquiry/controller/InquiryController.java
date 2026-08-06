package com.monsterhouse.inquiry.controller;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.inquiry.dto.request.InquiryCreateRequest;
import com.monsterhouse.inquiry.dto.response.InquiryCreateResponse;
import com.monsterhouse.inquiry.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    /** 통역 신청 · 영상 문의 공용 (기획서 §6.5) */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InquiryCreateResponse> create(
            @Valid @RequestBody InquiryCreateRequest request) {

        LocaleCode locale = LocaleCode.from(LocaleContextHolder.getLocale());
        return ApiResponse.ok(inquiryService.create(request, locale));
    }
}
