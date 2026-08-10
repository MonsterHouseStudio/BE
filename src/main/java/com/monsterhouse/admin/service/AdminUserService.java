package com.monsterhouse.admin.service;

import com.monsterhouse.admin.dto.AdminProfileResponse;
import com.monsterhouse.admin.dto.AdminUserCreateRequest;
import com.monsterhouse.admin.dto.PasswordChangeRequest;
import com.monsterhouse.admin.entity.AdminUser;
import com.monsterhouse.admin.repository.AdminUserRepository;
import com.monsterhouse.admin.repository.RefreshTokenRepository;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {
    private final AdminUserRepository adminUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    public List<AdminProfileResponse> findAll(){
        return adminUserRepository.findAll().stream().map(AdminProfileResponse::of).toList();
    }
    @Transactional
    public AdminProfileResponse create(AdminUserCreateRequest request){
        if(adminUserRepository.existsByUsername(request.username())){
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
        AdminUser admin = adminUserRepository.save(AdminUser.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .role(request.role())
                .build());
        log.info("Admin user created. username={} role={}", admin.getUsername(), admin.getRole());
        return AdminProfileResponse.of(admin);
    }
    //pw chng
    @Transactional
    public void changePassword(Long adminUserId, PasswordChangeRequest request){
        AdminUser admin = getOrThrow(adminUserId);
        if(!passwordEncoder.matches(request.currentPassword(), admin.getPasswordHash())){
            log.info("Password change rejected - wrong current password. id={}", adminUserId);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if(request.currentPassword().equals(request.newPassword())){
            throw new BusinessException(ErrorCode.SAME_PASSWORD);
        }
        admin.changePassword(passwordEncoder.encode(request.newPassword()));
        int revoked = refreshTokenRepository.revokeAllByAdminUserId(adminUserId, LocalDateTime.now());
        log.info("Passowrd changed. id={} revokedSession={}", adminUserId, revoked);
    }


    //계정 비활성화.
    @Transactional
    public void disable(Long adminUserId, Long requesterId){
        if (adminUserId.equals(requesterId)){
            throw new BusinessException(ErrorCode.CANNOT_DISABLE_SELF);
        }
        AdminUser admin = getOrThrow(adminUserId);
        admin.disable();
        refreshTokenRepository.revokeAllByAdminUserId(adminUserId, LocalDateTime.now());
        log.info("Admin user disabled. id={} by={}", adminUserId, requesterId);
    }
    private AdminUser getOrThrow(Long adminUserId){
        return adminUserRepository.findById(adminUserId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
