package com.monsterhouse.admin.dto;

import com.monsterhouse.admin.entity.AdminRole;
import com.monsterhouse.admin.entity.AdminUser;

import java.time.LocalDateTime;

public record AdminProfileResponse(
        Long id,
        String username,
        String displayName,
        AdminRole role,
        LocalDateTime lastLoginAt
) {
    public static AdminProfileResponse of(AdminUser admin){
        return new AdminProfileResponse(
                admin.getId(),
                admin.getUsername(),
                admin.getDisplayName(),
                admin.getRole(),
                admin.getLastLoginAt()
        );
    }
}
