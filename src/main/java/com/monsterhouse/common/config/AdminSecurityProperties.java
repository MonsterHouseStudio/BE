package com.monsterhouse.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin")
public record AdminSecurityProperties (
        int maxLoginAttempts,
        int lockMinutes,
        Seed seed
){
    public record Seed(boolean enabled, String username, String password){
    }
    public boolean seedEnabled(){
        return seed != null
                && seed.enabled()
                && seed.username() != null
                && seed.password() != null;
    }
}
