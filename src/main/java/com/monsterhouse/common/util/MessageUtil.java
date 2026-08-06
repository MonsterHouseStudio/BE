package com.monsterhouse.common.util;

import com.monsterhouse.common.enums.LocaleCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageUtil {
    private final MessageSource messageSource;
    public String get(String key, Object... args){
        return get(key, LocaleContextHolder.getLocale(), args);
    }
    public String get(String key, LocaleCode localeCode, Object... args){
        return get(key, localeCode.getLocale(), args);
    }
    /**
     * 번들에 키가 없으면 예외 대신 키 문자열을 그대로 돌려줍니다(운영 중 500 방지).
     * 다만 그러면 사용자 화면에 "error.product.in.use" 같은 문자열이 그대로 노출되고도
     * 아무도 모르게 됩니다. 실제로 그 상태로 배포될 뻔했습니다.
     * → 폴백이 발동하면 WARN 을 남겨 로그에서 드러나게 합니다.
     */
    public String get(String key, Locale locale, Object... args){
        String resolved = messageSource.getMessage(key, args, key, locale);

        if (resolved.equals(key)) {
            log.warn("메시지 키를 찾지 못했습니다. messages_{}.properties 에 '{}' 를 추가하세요.",
                    locale.getLanguage(), key);
        }
        return resolved;
    }
}
