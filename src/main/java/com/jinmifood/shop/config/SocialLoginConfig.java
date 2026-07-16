package com.jinmifood.shop.config;

import com.jinmifood.shop.config.properties.SocialLoginProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.*;
import java.util.*;

@Configuration
public class SocialLoginConfig {
    @Bean ClientRegistrationRepository clientRegistrationRepository(SocialLoginProperties properties){
        var registrations=new ArrayList<ClientRegistration>();
        if(properties.getGoogle().isConfigured())registrations.add(google(properties.getGoogle()));
        if(properties.getKakao().isConfigured())registrations.add(kakao(properties.getKakao()));
        if(properties.getNaver().isConfigured())registrations.add(naver(properties.getNaver()));
        return new OptionalClientRegistrationRepository(registrations);
    }
    private ClientRegistration google(SocialLoginProperties.Provider p){
        return ClientRegistration.withRegistrationId("google").clientName("Google").clientId(p.getClientId()).clientSecret(p.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}").scope("openid","profile","email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth").tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo").userNameAttributeName("sub")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build();
    }
    private ClientRegistration kakao(SocialLoginProperties.Provider p){
        return ClientRegistration.withRegistrationId("kakao").clientName("카카오").clientId(p.getClientId()).clientSecret(p.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                // Email is unavailable until Kakao approves the app as a Biz app.
                // The stable provider user ID is used for identity; request only
                // the nickname that is actually consumed by the storefront.
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}").scope("profile_nickname")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize").tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me").userNameAttributeName("id").build();
    }
    private ClientRegistration naver(SocialLoginProperties.Provider p){
        return ClientRegistration.withRegistrationId("naver").clientName("네이버").clientId(p.getClientId()).clientSecret(p.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}").scope("name","email")
                .authorizationUri("https://nid.naver.com/oauth2.0/authorize").tokenUri("https://nid.naver.com/oauth2.0/token")
                .userInfoUri("https://openapi.naver.com/v1/nid/me").userNameAttributeName("response").build();
    }

    static final class OptionalClientRegistrationRepository implements ClientRegistrationRepository,Iterable<ClientRegistration> {
        private final Map<String,ClientRegistration> registrations;
        OptionalClientRegistrationRepository(List<ClientRegistration> values){var map=new LinkedHashMap<String,ClientRegistration>();values.forEach(v->map.put(v.getRegistrationId(),v));registrations=Map.copyOf(map);}
        @Override public ClientRegistration findByRegistrationId(String registrationId){return registrations.get(registrationId);}
        @Override public Iterator<ClientRegistration> iterator(){return registrations.values().iterator();}
    }
}
