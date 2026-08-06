package com.monsterhouse.admin;
import com.monsterhouse.admin.entity.AdminRole;
import com.monsterhouse.admin.entity.AdminUser;
import com.monsterhouse.admin.repository.AdminUserRepository;
import com.monsterhouse.common.config.AdminSecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개발용 초기 관리자 계정.
 *
 * @Profile("local") 을 반드시 유지하세요.
 * 운영에 이게 살아 있으면 아이디·비밀번호가 알려진 계정이 자동 생성됩니다.
 * 운영 계정은 배포 후 수동 생성하거나 별도 마이그레이션으로 넣습니다.
 */
@Slf4j
@Component
@Profile("local")
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
        log.warn("[local] 초기 관리자 계정을 생성했습니다. username={} - 운영에서는 절대 사용 금지", username);
    }
}
