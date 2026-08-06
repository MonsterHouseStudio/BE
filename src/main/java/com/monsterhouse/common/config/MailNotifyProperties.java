package com.monsterhouse.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailNotifyProperties(
        boolean enabled,
        String from,
        String adminTo
) {
}
