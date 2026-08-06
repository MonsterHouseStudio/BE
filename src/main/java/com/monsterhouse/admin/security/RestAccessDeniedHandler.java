package com.monsterhouse.admin.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monsterhouse.common.exception.ErrorCode;
import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.common.util.MessageUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;
    private final MessageUtil messageUtil;
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(ErrorCode.FORBIDDEN.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.fail(
                        ErrorCode.FORBIDDEN.getCode(),
                        messageUtil.get(ErrorCode.FORBIDDEN.getMessageKey())
                )
        );
    }
}