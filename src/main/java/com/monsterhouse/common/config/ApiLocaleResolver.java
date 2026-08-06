package com.monsterhouse.common.config;

import com.monsterhouse.common.enums.LocaleCode;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

public class ApiLocaleResolver implements LocaleResolver {
    private static final String HEADER = "X-Locale";
    private static final String PARAM = "locale";
    @Override
    @NonNull
    public Locale resolveLocale(@NonNull HttpServletRequest request){
        String fromHeader = request.getHeader(HEADER);
        if(fromHeader != null && !fromHeader.isBlank()){
            return LocaleCode.from(fromHeader).getLocale();
        }
        String fromParam = request.getParameter(PARAM);
        if(fromParam != null && !fromParam.isBlank()){
            return LocaleCode.from(fromParam).getLocale();
        }
        String acceptLanguage = request.getHeader("Accept-Language");
        if(acceptLanguage != null && !acceptLanguage.isBlank()){
            return LocaleCode.from(acceptLanguage).getLocale();
        }
        return LocaleCode.DEFAULT.getLocale();
    }
    @Override
    public void setLocale(@NonNull HttpServletRequest request,
                          @Nullable HttpServletResponse response,
                          @Nullable Locale locale){
        throw new UnsupportedOperationException("언어는 요청 단위로 판별, 서버 세션에 저장x");
    }
}
