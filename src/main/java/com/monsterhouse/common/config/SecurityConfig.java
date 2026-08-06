package com.monsterhouse.common.config;

import com.monsterhouse.admin.security.JwtAuthenticationFilter;
import com.monsterhouse.admin.security.RestAccessDeniedHandler;
import com.monsterhouse.admin.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 일반 사용자는 비회원(기획서 §4.2) → 공개 API 는 인증 없음.
 * /api/admin/** 만 JWT 로 보호합니다.
 *
 * CSRF 를 끈 근거:
 *   - 세션을 쓰지 않고(STATELESS) 보호된 API 는 Authorization 헤더로 인증합니다.
 *     헤더는 브라우저가 크로스사이트 요청에 자동으로 붙이지 않으므로 CSRF 가 성립하지 않습니다.
 *   - 쿠키로 인증되는 유일한 지점인 /api/admin/auth/refresh 는
 *     SameSite 쿠키 속성으로 크로스사이트 전송을 차단합니다(RefreshTokenCookie 참고).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // @PreAuthorize("hasRole('SUPER_ADMIN')") 사용
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})                       // WebConfig 의 CORS 매핑을 사용
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())     // 로그아웃은 AdminAuthController 가 처리
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // --- 관리자 인증 진입점 (토큰 없이 접근 가능해야 함) ---
                        .requestMatchers(HttpMethod.POST,
                                "/api/admin/auth/login",
                                "/api/admin/auth/refresh",
                                "/api/admin/auth/logout").permitAll()

                        // --- 그 외 관리자 API ---
                        .requestMatchers("/api/admin/**").authenticated()

                        // --- 공개 API ---
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/bookings",
                                "/api/bookings/*/cancel",
                                "/api/inquiries").permitAll()

                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/sitemap.xml", "/robots.txt").permitAll()
                        // 로컬 저장소로 올린 이미지 서빙 경로.
                        // /api 로 시작하지 않아 위의 GET permitAll 에 걸리지 않고,
                        // 빠뜨리면 anyRequest().denyAll() 에 막혀 갤러리 이미지가 전부 403 이 됩니다.
                        // (운영은 S3/CloudFront 가 서빙하므로 이 경로를 타지 않습니다)
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                        // CORS preflight 는 인증 대상이 아닙니다.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .anyRequest().denyAll()
                )

                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}