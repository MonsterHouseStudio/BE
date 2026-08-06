package com.monsterhouse.admin.repository;

import com.monsterhouse.admin.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update RefreshToken t
               set t.revokedAt = :now
             where t.adminUserId = :adminUserId
               and t.revokedAt is null
            """)
    int revokeAllByAdminUserId(@Param("adminUserId") Long adminUserId,
                               @Param("now") LocalDateTime now);

    /** 만료·폐기된 토큰 정리 배치용 */
    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :threshold")
    int deleteExpiredBefore(@Param("threshold") LocalDateTime threshold);
}