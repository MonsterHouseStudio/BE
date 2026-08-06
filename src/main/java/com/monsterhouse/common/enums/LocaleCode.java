package com.monsterhouse.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Locale;

/**
 * 이 서비스가 지원하는 언어. 기본은 한국어.
 * 콘텐츠 번역(post_translation 등)의 locale 컬럼과 동일한 값을 씁니다.
 */
@Getter
@RequiredArgsConstructor
public enum LocaleCode {
    KO("ko", Locale.KOREAN),
    JA("ja", Locale.JAPANESE);
    private final String code;
    private final Locale locale;
    public static final LocaleCode DEFAULT = KO;
    public static LocaleCode from(String code){
        if(code == null || code.isBlank()){
            return DEFAULT;
        }
        String lang = code.trim().toLowerCase().split("[-_]")[0];
        return Arrays.stream(values())
                .filter(it -> it.code.equals(lang))
                .findFirst()
                .orElse(DEFAULT);
    }
    public static LocaleCode from(Locale locale){
        return locale == null ? DEFAULT : from(locale.getLanguage());
    }
    public boolean isDefault(){
        return this == DEFAULT;
    }
}
