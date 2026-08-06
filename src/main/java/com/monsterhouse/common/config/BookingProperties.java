package com.monsterhouse.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.booking")
public record BookingProperties(
        int slotStepMin,
        int bufferMin,
        int maxAdvanceDays,
        int minLeadHours,
        boolean sharedResource,
        boolean autoConfirm
) {
}
