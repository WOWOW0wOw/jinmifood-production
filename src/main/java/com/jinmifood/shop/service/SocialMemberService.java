package com.jinmifood.shop.service;

import com.jinmifood.shop.domain.*;
import com.jinmifood.shop.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class SocialMemberService {
    private final MemberRepository members;private final SocialAccountRepository accounts;private final PasswordEncoder encoder;
    public SocialMemberService(MemberRepository members,SocialAccountRepository accounts,PasswordEncoder encoder){this.members=members;this.accounts=accounts;this.encoder=encoder;}

    @Transactional
    public Member login(String provider,SocialProfile profile){
        return accounts.findByProviderAndProviderUserId(provider,profile.providerUserId()).map(SocialAccount::getMember).orElseGet(()->create(provider,profile));
    }
    private Member create(String provider,SocialProfile profile){
        String supplied=profile.email()==null?"":profile.email().trim().toLowerCase();
        Member member=null;
        if(profile.verifiedEmail()&&!supplied.isBlank())member=members.findByEmailIgnoreCase(supplied).orElse(null);
        if(member==null){
            String email=!supplied.isBlank()&&!members.existsByEmailIgnoreCase(supplied)?supplied:syntheticEmail(provider,profile.providerUserId());
            String name=profile.name()==null||profile.name().isBlank()?provider+" 회원":profile.name().trim();
            if(name.length()>40)name=name.substring(0,40);
            member=members.save(new Member(email,encoder.encode(java.util.UUID.randomUUID().toString()),name,""));
        }
        accounts.save(new SocialAccount(provider,profile.providerUserId(),member));return member;
    }
    @Transactional
    public Member completeVerifiedIdentity(String email,String name,java.time.LocalDate birthDate,String phone){
        Member member=members.findByEmailForUpdate(email).orElseThrow(()->new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        if(!member.getPhone().isBlank()&&member.getBirthDate()!=null)return member;
        if(members.existsByPhoneAndIdNot(phone,member.getId()))throw new IllegalStateException("이미 다른 회원이 사용 중인 휴대전화번호입니다.");
        member.updateVerifiedIdentity(name,birthDate,phone);
        return member;
    }
    private String syntheticEmail(String provider,String id){
        try{var digest=MessageDigest.getInstance("SHA-256").digest(id.getBytes(StandardCharsets.UTF_8));return provider+"-"+HexFormat.of().formatHex(digest,0,12)+"@social.jinmifood.local";}
        catch(Exception e){throw new IllegalStateException("소셜 계정 식별자를 처리할 수 없습니다.",e);}
    }
    public record SocialProfile(String providerUserId,String email,String name,boolean verifiedEmail){}
}
