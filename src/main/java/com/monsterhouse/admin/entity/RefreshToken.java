package com.monsterhouse.admin.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 리프레시 토큰. 원문이 아니라 SHA-256 해시만 저장합니다.
 * DB 가 유출되어도 그 값으로는 토큰을 재현할 수 없습니다.
 *
 * revokedAt 이 채워진 토큰이 다시 들어오면 = 탈취된 토큰이 재사용된 것.
 * 그때 해당 계정의 모든 토큰을 끊습니다(AdminAuthService 참고).
 */
@Getter
@Entity
@Table(name = "refresh_token", uniqueConstraints = @UniqueConstraint(name = "uk_refresh_token_hash", columnNames = "token_hash"), indexes = @Index(name = "idx_refresh_token_admin", columnList = "admin_user_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "admin_user_id", nullable = false)
    private Long adminUserId;
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;
    @Column(name = "expires_at", nullable =false)
    private LocalDateTime expiresAt;
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    public RefreshToken(Long adminUserId, String tokenHash, LocalDateTime expiresAt){
        this.adminUserId = adminUserId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }
    public boolean isRevoked(){
        return revokedAt != null;
    }
    public boolean isExpired(LocalDateTime now){
        return expiresAt.isBefore(now);
    }
    public boolean isUsable(LocalDateTime now){
        return !isRevoked() && !isExpired(now);
    }
    public void revoke(LocalDateTime now){
        if(this.revokedAt == null){
            this.revokedAt = now;
        }
    }
}
