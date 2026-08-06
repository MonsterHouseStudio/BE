package com.monsterhouse.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.youtube")
public record YoutubeProperties(
        String apiKey,
        String channelId,
        int cacheTtlMinutes,
        int maxResults
) {

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && channelId != null && !channelId.isBlank();
    }
}
