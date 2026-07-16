package com.jinmifood.shop.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SocialOidcUserService extends OidcUserService {
    private final SocialMemberService members;
    public SocialOidcUserService(SocialMemberService members){this.members=members;}
    @Override public OidcUser loadUser(OidcUserRequest request)throws OAuth2AuthenticationException{
        OidcUser loaded=super.loadUser(request);String provider=request.getClientRegistration().getRegistrationId();
        var member=members.login(provider,new SocialMemberService.SocialProfile(loaded.getSubject(),loaded.getEmail(),loaded.getFullName(),Boolean.TRUE.equals(loaded.getEmailVerified())));
        if(!member.isActive())throw new OAuth2AuthenticationException(new OAuth2Error("account_disabled"),"사용 중지된 계정입니다.");
        var claims=new LinkedHashMap<String,Object>(loaded.getClaims());claims.put("memberEmail",member.getEmail());
        var authorities=new ArrayList<GrantedAuthority>();authorities.addAll(loaded.getAuthorities());authorities.add(new SimpleGrantedAuthority("ROLE_MEMBER"));if(member.isAdmin())authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        return new DefaultOidcUser(authorities,loaded.getIdToken(),new OidcUserInfo(claims),"memberEmail");
    }
}
