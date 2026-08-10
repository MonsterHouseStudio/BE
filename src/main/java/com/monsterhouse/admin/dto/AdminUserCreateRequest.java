package com.monsterhouse.admin.dto;

import com.monsterhouse.admin.entity.AdminRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public record AdminUserCreateRequest(
        @NotBlank
        @Size(min = 4, max = 50)
        @Pattern(regexp = "^[a-z0-9._-]+$", message = "아이디는 영문 소문자, 숫자, . _ - 만 사용할 수 있습니다.")
        String username,
        @NotBlank
        @Size(min = 10, max = 100)
        String password,
        @NotBlank
        @Size(max = 50)
        String displayName,
        @NotNull
        AdminRole role
) {
}
