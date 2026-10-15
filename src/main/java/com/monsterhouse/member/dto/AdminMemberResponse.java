package com.monsterhouse.member.dto;

import com.monsterhouse.member.entity.Member;

import java.time.LocalDateTime;

public record AdminMemberResponse(
        Long id,
        String email,
        String name,
        String phone,
        LocalDateTime createdAt
) {
    public static AdminMemberResponse of(Member m) {
        return new AdminMemberResponse(m.getId(), m.getEmail(), m.getName(), m.getPhone(), m.getCreatedAt());
    }
}
