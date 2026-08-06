package com.monsterhouse.inquiry.dto.request;

import com.monsterhouse.inquiry.entity.InquiryType;
import jakarta.validation.constraints.*;

public record InquiryCreateRequest(

        @NotNull
        InquiryType type,

        @NotBlank(message = "{valid.name.required}")
        @Size(max = 50)
        String name,

        @NotBlank(message = "{valid.phone.required}")
        @Size(max = 100)
        String contact,

        @NotBlank(message = "{valid.email.required}")
        @Email(message = "{valid.email.format}")
        @Size(max = 200)
        String email,

        @NotBlank(message = "{valid.content.required}")
        @Size(max = 2000)
        String content,

        @AssertTrue(message = "{valid.privacy.required}")
        boolean privacyAgreed,

        String website
) {

    public boolean isBot() {
        return website != null && !website.isBlank();
    }
}