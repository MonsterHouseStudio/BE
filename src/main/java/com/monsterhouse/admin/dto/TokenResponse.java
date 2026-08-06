package com.monsterhouse.admin.dto;

import com.monsterhouse.admin.entity.AdminRole;

public record TokenResponse(
        String accessToken,
        long expiresInSeconds,
        String username,
        String displayName,
        AdminRole role
) {
}
