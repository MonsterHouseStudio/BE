package com.monsterhouse.member.dto;

import com.monsterhouse.member.entity.Member;

public record MemberResponse(
        Long id,
        String email,
        String name,
        String phone
) {
    public static MemberResponse of(Member m) {
        return new MemberResponse(m.getId(), m.getEmail(), m.getName(), m.getPhone());
    }
}
