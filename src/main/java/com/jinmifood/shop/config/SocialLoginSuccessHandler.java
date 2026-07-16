package com.jinmifood.shop.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Removes provider access and refresh tokens as soon as the login profile is loaded. */
@Component
public class SocialLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    private final OAuth2AuthorizedClientRepository authorizedClients;

    public SocialLoginSuccessHandler(OAuth2AuthorizedClientRepository authorizedClients) {
        this.authorizedClients = authorizedClients;
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(false);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (authentication instanceof OAuth2AuthenticationToken social) {
            authorizedClients.removeAuthorizedClient(
                    social.getAuthorizedClientRegistrationId(), authentication, request, response);
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
