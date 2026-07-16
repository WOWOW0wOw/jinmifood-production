package com.jinmifood.shop.config;

import com.jinmifood.shop.config.properties.AdminProperties;
import com.jinmifood.shop.domain.Member;
import com.jinmifood.shop.repository.MemberRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class ConfiguredAdminInitializer implements ApplicationRunner {
    private final MemberRepository members;
    private final PasswordEncoder encoder;
    private final AdminProperties properties;

    public ConfiguredAdminInitializer(MemberRepository members, PasswordEncoder encoder,
            AdminProperties properties) {
        this.members = members;
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String username = properties.getUsername().trim().toLowerCase();
        Member admin = members.findByEmailIgnoreCase(username)
                .orElseGet(() -> new Member(username, encoder.encode(properties.getPassword()), "운영 관리자", ""));
        if (!encoder.matches(properties.getPassword(), admin.getPasswordHash())) {
            admin.changePassword(encoder.encode(properties.getPassword()));
        }
        admin.grantAdmin();
        admin.activate();
        members.save(admin);
    }
}
