package com.monsterhouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class MonsterHouseApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonsterHouseApplication.class, args);
    }
}