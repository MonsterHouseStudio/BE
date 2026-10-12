package com.monsterhouse.member.entity;

import com.monsterhouse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 고객 회원 (이메일 + 비밀번호). 예약은 이메일로 연결됩니다. */
@Getter
@Entity
@Table(name = "member", uniqueConstraints = @UniqueConstraint(name = "uk_member_email", columnNames = "email"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 200)
    private String email;

    /** BCrypt 해시. 평문은 어디에도 저장하지 않습니다. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "phone", nullable = false, length = 30)
    private String phone;

    @Builder
    private Member(String email, String passwordHash, String name, String phone) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.phone = phone;
    }

    public void updateProfile(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
