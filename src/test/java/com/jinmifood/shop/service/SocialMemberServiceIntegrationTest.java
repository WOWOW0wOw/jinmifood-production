package com.jinmifood.shop.service;

import com.jinmifood.shop.repository.SocialAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SocialMemberServiceIntegrationTest {
    @Autowired SocialMemberService socialMembers;
    @Autowired SocialAccountRepository accounts;

    @Test void repeatSocialLoginReturnsAnInitializedMemberOutsideTheTransaction(){
        String providerUserId="repeat-login-integration-user";
        var profile=new SocialMemberService.SocialProfile(providerUserId,"repeat-social@example.com","반복 로그인",true);
        var created=socialMembers.login("google",profile);
        var loaded=socialMembers.login("google",profile);

        assertThat(loaded.getId()).isEqualTo(created.getId());
        assertThat(loaded.getEmail()).isEqualTo("repeat-social@example.com");
        assertThat(loaded.isActive()).isTrue();
        assertThat(accounts.findByProviderAndProviderUserId("google",providerUserId)).isPresent();
    }
}
