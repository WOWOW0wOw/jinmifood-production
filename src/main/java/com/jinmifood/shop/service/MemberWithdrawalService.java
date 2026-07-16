package com.jinmifood.shop.service;

import com.jinmifood.shop.domain.Member;
import com.jinmifood.shop.domain.MemberAdminAction;
import com.jinmifood.shop.repository.MemberAdminActionRepository;
import com.jinmifood.shop.repository.MemberRepository;
import com.jinmifood.shop.repository.SocialAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MemberWithdrawalService {
    public static final String CONFIRMATION="탈퇴합니다";
    private final MemberRepository members;
    private final SocialAccountRepository socialAccounts;
    private final MemberAdminActionRepository actions;
    private final PasswordEncoder encoder;

    public MemberWithdrawalService(MemberRepository members,SocialAccountRepository socialAccounts,
        MemberAdminActionRepository actions,PasswordEncoder encoder){
        this.members=members;this.socialAccounts=socialAccounts;this.actions=actions;this.encoder=encoder;
    }

    @Transactional
    public void withdrawSelf(String email,String currentPassword,String confirmation,boolean socialSession){
        if(!CONFIRMATION.equals(confirmation))throw new IllegalArgumentException("확인 문구에 '탈퇴합니다'를 정확히 입력해 주세요.");
        Member member=members.findByEmailForUpdate(email).orElseThrow();
        if(member.isAdmin())throw new IllegalStateException("관리자 계정은 권한을 해제한 후 탈퇴할 수 있습니다.");
        if(!socialSession&&(currentPassword==null||!encoder.matches(currentPassword,member.getPasswordHash())))
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        withdraw(member,"SELF","본인 요청");
    }

    @Transactional
    public void withdrawByAdmin(Long memberId,String actor,String reason){
        if(reason==null||reason.isBlank())throw new IllegalArgumentException("강제 탈퇴 사유를 입력해 주세요.");
        Member member=members.findByIdForUpdate(memberId).orElseThrow();
        if(member.getEmail().equalsIgnoreCase(actor))throw new IllegalStateException("자기 계정은 관리자 화면에서 탈퇴시킬 수 없습니다.");
        if(member.isAdmin()&&member.isActive()&&members.countByAdminTrueAndActiveTrue()<=1)
            throw new IllegalStateException("마지막 활성 관리자는 탈퇴시킬 수 없습니다.");
        withdraw(member,actor,reason.trim());
    }

    private void withdraw(Member member,String actor,String reason){
        if(member.isWithdrawn())throw new IllegalStateException("이미 탈퇴한 회원입니다.");
        actions.save(new MemberAdminAction(member,trim(actor,120),"WITHDRAWAL","ACTIVE","WITHDRAWN",trim(reason,200)));
        socialAccounts.deleteByMemberId(member.getId());
        String token=member.getId()+"-"+UUID.randomUUID().toString().replace("-","");
        member.withdraw("withdrawn-"+token+"@deleted.invalid",encoder.encode(UUID.randomUUID().toString()),LocalDateTime.now());
    }

    private String trim(String value,int max){String safe=value==null?"":value.trim();return safe.substring(0,Math.min(safe.length(),max));}
}
