package com.monsterhouse.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
    /**
     * 기동 시 검증.
     *
     * WebConfig 가 allowCredentials(true) 와 함께 allowedOriginPatterns 를 씁니다.
     * 이 조합에서 "모든 오리진을 반영하는" 값(*, http://*, https://*)이 들어오면
     * 아무 사이트나 자격증명 요청을 보낼 수 있게 되는 전형적 취약 조합이 됩니다.
     * 그래서 그런 값이 설정되면 조용히 여는 대신 기동을 거부합니다.
     *
     * 서브도메인 와일드카드(https://*.example.com)와 포트 와일드카드
     * (http://localhost:*)는 호스트가 특정되므로 허용합니다.
     */
    public CorsProperties {
        if (allowedOrigins != null) {
            for (String origin : allowedOrigins) {
                String host = origin.replaceFirst("^\\w+://", "");
                if (origin.equals("*") || host.equals("*") || host.startsWith("*:")) {
                    throw new IllegalStateException(
                            "app.cors.allowed-origins 에 모든 오리진을 반영하는 '" + origin
                            + "' 를 쓸 수 없습니다. allowCredentials(true) 와 함께면 "
                            + "자격증명이 탈취됩니다. 정확한 오리진이나 서브도메인 "
                            + "와일드카드(https://*.example.com)를 쓰세요.");
                }
            }
        }
    }
}
