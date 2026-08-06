package com.monsterhouse.admin.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monsterhouse.common.exception.ErrorCode;
import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.common.util.MessageUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증이 필요한데 없거나 깨진 경우.
 * 기본 동작(빈 401 또는 로그인 폼 리다이렉트) 대신
 * 나머지 API 와 똑같은 ApiResponse 형태로 내려 프론트 처리를 통일합니다.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final MessageUtil messageUtil;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        // 필터가 남긴 사유가 있으면 그것을, 없으면 일반 인증 필요로 응답합니다.
        Object attribute = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        ErrorCode errorCode = (attribute instanceof ErrorCode code) ? code : ErrorCode.UNAUTHORIZED;

        write(response, errorCode);
    }

    private void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.fail(errorCode.getCode(), messageUtil.get(errorCode.getMessageKey()))
        );
    }
}