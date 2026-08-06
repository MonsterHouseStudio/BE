package com.monsterhouse.inquiry.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.inquiry.entity.Inquiry;
import com.monsterhouse.inquiry.entity.InquiryStatus;
import com.monsterhouse.inquiry.entity.InquiryType;

import java.time.LocalDateTime;

public record AdminInquiryResponse(
        Long id,
        InquiryType type,
        String name,
        String contact,
        String email,
        LocaleCode locale,
        String content,
        InquiryStatus status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime handledAt,
        String handledBy,
        String adminMemo
) {
    public static AdminInquiryResponse of(Inquiry inquiry){
        return new AdminInquiryResponse(
                inquiry.getId(),
                inquiry.getType(),
                inquiry.getName(),
                inquiry.getContact(),
                inquiry.getEmail(),
                inquiry.getLocale(),
                inquiry.getContent(),
                inquiry.getStatus(),
                inquiry.getCreatedAt(),
                inquiry.getHandledAt(),
                inquiry.getHandledBy(),
                inquiry.getAdminMemo()
        );
    }
}
