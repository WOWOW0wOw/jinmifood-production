package com.jinmifood.shop.config;

import com.jinmifood.shop.config.properties.AdminProperties;
import com.jinmifood.shop.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ConfiguredAdminInitializerTest {
    @Autowired MemberRepository members;

    @Test
    void provisionsAndRotatesTheConfiguredAdministrator() throws Exception {
        var encoder = new BCryptPasswordEncoder();
        var properties = properties("first-strong-password");
        new ConfiguredAdminInitializer(members, encoder, properties).run(null);

        var created = members.findByEmailIgnoreCase("ops-admin").orElseThrow();
        assertThat(created.isAdmin()).isTrue();
        assertThat(created.isActive()).isTrue();
        assertThat(encoder.matches("first-strong-password", created.getPasswordHash())).isTrue();

        properties.setPassword("rotated-strong-password");
        new ConfiguredAdminInitializer(members, encoder, properties).run(null);

        var rotated = members.findByEmailIgnoreCase("ops-admin").orElseThrow();
        assertThat(members.count()).isOne();
        assertThat(encoder.matches("rotated-strong-password", rotated.getPasswordHash())).isTrue();
    }

    private AdminProperties properties(String password) {
        var properties = new AdminProperties();
        properties.setUsername("Ops-Admin");
        properties.setPassword(password);
        return properties;
    }
}
