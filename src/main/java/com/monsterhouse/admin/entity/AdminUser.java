package com.monsterhouse.admin.entity;

import com.monsterhouse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관리자 계정. 일반 사용자는 비회원이므로(기획서 §4.2) 회원 테이블은 이것 하나뿐입니다.
 *
 * 로그인 시도 제한(기획서 §9)을 DB 필드로 구현한 이유:
 *   메모리 카운터는 서버 재시작·스케일아웃 시 초기화되어 우회됩니다.
 */
@Getter
@Entity
@Table(
        name = "admin_user",
        uniqueConstraints = @UniqueConstraint(name = "uk_admin_user_username", columnNames = "username")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminUser extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    /** BCrypt 해시. 평문은 어디에도 남기지 않습니다. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private AdminRole role;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    /** null 이거나 과거면 잠기지 않은 상태 */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Builder
    private AdminUser(String username, String passwordHash, String displayName, AdminRole role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.enabled = true;
        this.failedLoginCount = 0;
    }

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /** 잠금이 풀릴 때까지 남은 분. 사용자 안내 메시지에 씁니다. */
    public long remainingLockMinutes(LocalDateTime now) {
        if (!isLocked(now)) {
            return 0;
        }
        return Math.max(1, java.time.Duration.between(now, lockedUntil).toMinutes() + 1);
    }

    /**
     * 로그인 실패 기록. 임계치에 닿으면 계정을 잠급니다.
     * 잠금이 걸리는 순간 카운터를 0으로 되돌려, 잠금 해제 후 한 번 더 틀렸다고
     * 즉시 재잠금되는 일이 없게 합니다.
     */
    public void recordLoginFailure(int maxAttempts, int lockMinutes, LocalDateTime now) {
        this.failedLoginCount++;
        if (this.failedLoginCount >= maxAttempts) {
            this.lockedUntil = now.plusMinutes(lockMinutes);
            this.failedLoginCount = 0;
        }
    }

    public void recordLoginSuccess(LocalDateTime now) {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
        this.lastLoginAt = now;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void disable() {
        this.enabled = false;
    }
}