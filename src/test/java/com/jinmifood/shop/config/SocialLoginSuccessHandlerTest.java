package com.jinmifood.shop.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SocialLoginSuccessHandlerTest {
    @Test
    void removesProviderTokensImmediatelyAfterLogin() throws Exception {
        var clients = mock(OAuth2AuthorizedClientRepository.class);
        var handler = new SocialLoginSuccessHandler(clients);
        var principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER")), Map.of("id", "provider-user"), "id");
        var authentication = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "kakao");
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(clients).removeAuthorizedClient("kakao", authentication, request, response);
        assertThat(response.getRedirectedUrl()).isEqualTo("/");
    }
}
