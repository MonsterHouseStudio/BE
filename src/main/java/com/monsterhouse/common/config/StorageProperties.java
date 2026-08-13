package com.monsterhouse.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        /** local | s3 */
        String type,
        String localPath,
        String region,
        String bucket,
        String cdnDomain,
        int presignExpireMinutes,
        long maxImageBytes,
        int mediumWidth,
        int thumbWidth,
        long maxVideoBytes
) {

    public boolean isS3() {
        return "s3".equalsIgnoreCase(type);
    }
}
