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
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

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

                // --- 보안 헤더 ---
                // 순수 JSON API 라 CSP 는 "아무 리소스도 로드하지 않는다"로 잠급니다.
                // 이미지/HTML 을 직접 렌더링하지 않으므로 default-src 'none' 이 안전합니다.
                // HSTS 는 HTTPS 응답에만 붙습니다(로컬 HTTP 개발은 영향 없음).
                // X-Content-Type-Options(nosniff)·X-Frame-Options(DENY)는 Spring 기본 제공.
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000))   // 1년
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                )

                .authorizeHttpRequests(auth -> auth
                        // --- 관리자 인증 진입점 (토큰 없이 접근 가능해야 함) ---
                        .requestMatchers(HttpMethod.POST,
                                "/api/admin/auth/login",
                                "/api/admin/auth/refresh",
                                "/api/admin/auth/logout").permitAll()

                        // --- 그 외 관리자 API ---
                        .requestMatchers("/api/admin/**").authenticated()

                        // --- 공개 API ---
                        // ⚠ 지뢰: 모든 관리자 GET 은 반드시 /api/admin/** 안에 두세요.
                        //   그 밖(예: /api/dashboard)에 관리자 GET 을 만들면 이 줄에 걸려 즉시 공개됩니다.
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        // 비회원 예약이라 로그인이 없습니다. 본인 확인은
                        // "예약번호 + 이메일 일치"를 각 서비스 메서드가 직접 합니다.
                        // ⚠ 고객용 예약 API 를 새로 만들면 반드시 여기에 추가해야 합니다.
                        //   빠뜨리면 anyRequest().denyAll() 에 걸려 401 이 납니다.
                        .requestMatchers(HttpMethod.POST,
                                "/api/bookings",
                                "/api/bookings/*/cancel",
                                "/api/bookings/*/reschedule",
                                "/api/inquiries").permitAll()

                        // ★ /** 가 반드시 필요합니다.
                        //   "/actuator/health" 만 허용하면 정확히 그 경로만 열립니다.
                        //   쿠버네티스 프로브가 쓰는 하위 경로
                        //     /actuator/health/liveness
                        //     /actuator/health/readiness
                        //   는 anyRequest().denyAll() 에 걸려 401 이 됩니다.
                        //
                        //   그러면 파드가 영원히 Ready 가 되지 않아 서비스에 편입되지 못하고,
                        //   startupProbe 실패로 무한 재시작에 빠집니다.
                        //   앱 자체는 멀쩡히 떠 있어서 로그만 봐서는 원인을 못 찾습니다.
                        //   (실제로 k3s 에 올렸을 때 이 증상으로 막혔습니다)
                        //
                        //   health 그룹은 show-details: never 라 상태값만 노출되므로
                        //   공개해도 정보가 새지 않습니다.
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
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