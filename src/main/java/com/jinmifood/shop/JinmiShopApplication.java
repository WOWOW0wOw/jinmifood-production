package com.jinmifood.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class JinmiShopApplication {
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    static {
        TimeZone.setDefault(TimeZone.getTimeZone(BUSINESS_ZONE));
    }

    public static void main(String[] args) {
        SpringApplication.run(JinmiShopApplication.class, args);
    }
}
