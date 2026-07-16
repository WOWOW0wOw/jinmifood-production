package com.jinmifood.shop.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.*;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SocialOAuth2UserService extends DefaultOAuth2UserService {
    private final SocialMemberService members;
    public SocialOAuth2UserService(SocialMemberService members){this.members=members;}
    @Override public OAuth2User loadUser(OAuth2UserRequest request)throws OAuth2AuthenticationException{
        OAuth2User loaded=super.loadUser(request);String provider=request.getClientRegistration().getRegistrationId();
        var profile=profile(provider,loaded.getAttributes());var member=members.login(provider,profile);
        if(!member.isActive())throw new OAuth2AuthenticationException(new OAuth2Error("account_disabled"),"사용 중지된 계정입니다.");
        var attributes=new LinkedHashMap<String,Object>(loaded.getAttributes());attributes.put("memberEmail",member.getEmail());
        var authorities=new ArrayList<GrantedAuthority>();authorities.addAll(loaded.getAuthorities());authorities.add(new SimpleGrantedAuthority("ROLE_MEMBER"));if(member.isAdmin())authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        return new DefaultOAuth2User(authorities,attributes,"memberEmail");
    }
    @SuppressWarnings("unchecked")
    private SocialMemberService.SocialProfile profile(String provider,Map<String,Object> attrs){
        if(provider.equals("kakao")){
            var account=(Map<String,Object>)attrs.getOrDefault("kakao_account",Map.of());var p=(Map<String,Object>)account.getOrDefault("profile",Map.of());
            return new SocialMemberService.SocialProfile(String.valueOf(attrs.get("id")),string(account.get("email")),string(p.get("nickname")),Boolean.TRUE.equals(account.get("is_email_verified")));
        }
        var response=(Map<String,Object>)attrs.getOrDefault("response",Map.of());
        return new SocialMemberService.SocialProfile(string(response.get("id")),string(response.get("email")),first(string(response.get("name")),string(response.get("nickname"))),false);
    }
    private String string(Object v){return v==null?"":String.valueOf(v);} private String first(String a,String b){return a.isBlank()?b:a;}
}
