package com.jinmifood.shop.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="member_admin_actions",indexes={
    @Index(name="idx_member_admin_action_member",columnList="member_id,created_at"),
    @Index(name="idx_member_admin_action_created",columnList="created_at")
})
public class MemberAdminAction {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private Member member;
    @Column(nullable=false,length=120) private String actor;
    @Column(nullable=false,length=30) private String action;
    @Column(name="before_value",length=200) private String beforeValue;
    @Column(name="after_value",length=200) private String afterValue;
    @Column(length=200) private String reason;
    @Column(nullable=false,updatable=false) private LocalDateTime createdAt;

    protected MemberAdminAction(){}
    public MemberAdminAction(Member member,String actor,String action,String beforeValue,String afterValue,String reason){
        this.member=member;this.actor=actor;this.action=action;this.beforeValue=beforeValue;this.afterValue=afterValue;
        this.reason=reason;this.createdAt=LocalDateTime.now();
    }
    public Long getId(){return id;} public Member getMember(){return member;} public String getActor(){return actor;}
    public String getAction(){return action;} public String getBeforeValue(){return beforeValue;} public String getAfterValue(){return afterValue;}
    public String getReason(){return reason;} public LocalDateTime getCreatedAt(){return createdAt;}
}
