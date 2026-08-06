package com.monsterhouse.admin.security;

import com.monsterhouse.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authorization: Bearer <token> 을 읽어 SecurityContext 를 채웁니다.
 *
 * 토큰이 없거나 깨졌어도 여기서 응답을 만들지 않습니다.
 * 인증 없이 통과시키고, 보호된 경로면 EntryPoint 가 401 을 만듭니다.
 * (공개 API 에 실수로 Authorization 헤더가 붙어도 요청이 죽지 않게)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTRIBUTE = "mh.authError";

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            try {
                AdminPrincipal principal = tokenProvider.parseAccessToken(token);

                var authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority(principal.role().authority()))
                );
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JwtTokenProvider.TokenExpiredException e) {
                // 프론트가 이 코드를 보고 조용히 refresh 를 호출합니다.
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.TOKEN_EXPIRED);

            } catch (JwtTokenProvider.InvalidTokenException e) {
                log.debug("Invalid access token: {}", e.getMessage());
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.INVALID_TOKEN);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            String value = header.substring(PREFIX.length()).trim();
            return value.isEmpty() ? null : value;
        }
        return null;
    }
}