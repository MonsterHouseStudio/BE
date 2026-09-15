package com.monsterhouse.admin;
import com.monsterhouse.admin.entity.AdminRole;
import com.monsterhouse.admin.entity.AdminUser;
import com.monsterhouse.admin.repository.AdminUserRepository;
import com.monsterhouse.common.config.AdminSecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 초기 관리자 계정 시드.
 *
 * 기본은 꺼짐입니다(app.admin.seed 미설정 시 seedEnabled()=false).
 * - local 프로필: local yml 이 seed 를 켜서 개발용 계정(admin/admin1234!)을 만듭니다.
 * - 운영: 최초 1회만 APP_ADMIN_SEED_ENABLED=true + 강한 비밀번호로 계정을 만들고,
 *   로그인 후 다시 false 로 끕니다. 이미 있으면 재생성하지 않습니다(멱등).
 * ⚠ 운영에서 개발용 비밀번호(admin1234!)를 쓰지 마세요 — 반드시 강한 값으로.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner{
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSecurityProperties properties;
    @Override
    @Transactional
    public void run(ApplicationArguments args){
        if(!properties.seedEnabled()){
            return;
        }
        String username = properties.seed().username();
        if(adminUserRepository.existsByUsername(username)){
            return;
        }
        adminUserRepository.save(AdminUser.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(properties.seed().password()))
                .displayName("운영자")
                .role(AdminRole.SUPER_ADMIN)
                .build());
        log.warn("초기 관리자 계정을 생성했습니다. username={} — 로그인 후 시드를 끄세요(APP_ADMIN_SEED_ENABLED=false).", username);
    }
}
