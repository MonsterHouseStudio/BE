package com.monsterhouse.admin.security;

import com.monsterhouse.admin.entity.AdminRole;

public record AdminPrincipal(
        Long id,
        String username,
        AdminRole role
) {
}
