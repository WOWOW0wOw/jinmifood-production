package com.jinmifood.shop;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class KoreaTimeZoneTest {
    @Test
    void applicationUsesKoreaStandardTimeForBusinessTimestamps() {
        assertThat(TimeZone.getDefault().toZoneId()).isEqualTo(JinmiShopApplication.BUSINESS_ZONE);

        var utcNow = LocalDateTime.now(ZoneOffset.UTC);
        var businessNow = LocalDateTime.now();
        assertThat(java.time.Duration.between(utcNow, businessNow).toHours()).isBetween(8L, 9L);
    }
}
