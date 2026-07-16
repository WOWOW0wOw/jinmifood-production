package com.jinmifood.shop.service;

import com.jinmifood.shop.domain.Member;
import com.jinmifood.shop.domain.MemberAdminAction;
import com.jinmifood.shop.repository.MemberAdminActionRepository;
import com.jinmifood.shop.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberAdminService {
    private final MemberRepository members;
    private final MemberAdminActionRepository actions;

    public MemberAdminService(MemberRepository members,MemberAdminActionRepository actions){this.members=members;this.actions=actions;}

    @Transactional
    public void changeActive(Long memberId,boolean active,String actor,String reason){
        Member member=members.findByIdForUpdate(memberId).orElseThrow();
        preventSelf(member,actor,"자기 계정의 상태는 변경할 수 없습니다.");
        if(!active&&member.isAdmin()&&members.countByAdminTrueAndActiveTrue()<=1)
            throw new IllegalStateException("마지막 활성 관리자는 차단할 수 없습니다.");
        boolean before=member.isActive();
        if(active)member.activate();else member.deactivate();
        audit(member,actor,"ACCOUNT_STATUS",before?"ACTIVE":"BLOCKED",active?"ACTIVE":"BLOCKED",reason);
    }

    @Transactional
    public void changeAdmin(Long memberId,boolean admin,String actor,String reason){
        Member member=members.findByIdForUpdate(memberId).orElseThrow();
        preventSelf(member,actor,"자기 계정의 관리자 권한은 변경할 수 없습니다.");
        if(!admin&&member.isAdmin()&&member.isActive()&&members.countByAdminTrueAndActiveTrue()<=1)
            throw new IllegalStateException("마지막 활성 관리자의 권한은 해제할 수 없습니다.");
        boolean before=member.isAdmin();
        if(admin)member.grantAdmin();else member.revokeAdmin();
        audit(member,actor,"ADMIN_ROLE",String.valueOf(before),String.valueOf(admin),reason);
    }

    @Transactional
    public void adjustPoints(Long memberId,int delta,String actor,String reason){
        if(delta==0||Math.abs((long)delta)>1_000_000)throw new IllegalArgumentException("포인트 조정값은 0이 아니고 100만 포인트 이하여야 합니다.");
        if(reason==null||reason.isBlank())throw new IllegalArgumentException("포인트 조정 사유를 입력해 주세요.");
        Member member=members.findByIdForUpdate(memberId).orElseThrow();
        int before=member.getPoints();member.adjustPoints(delta);
        audit(member,actor,"POINTS",String.valueOf(before),String.valueOf(member.getPoints()),reason);
    }

    private void preventSelf(Member member,String actor,String message){if(member.getEmail().equalsIgnoreCase(actor))throw new IllegalStateException(message);}
    private void audit(Member member,String actor,String action,String before,String after,String reason){
        String safeActor=trim(actor,120);String safeReason=trim(reason,200);
        actions.save(new MemberAdminAction(member,safeActor,action,before,after,safeReason));
    }
    private String trim(String value,int max){if(value==null)return null;String v=value.trim();return v.length()>max?v.substring(0,max):v;}
}
