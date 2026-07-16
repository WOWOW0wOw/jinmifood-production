package com.jinmifood.shop.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="social_accounts",uniqueConstraints=@UniqueConstraint(name="uq_social_provider_user",columnNames={"provider","provider_user_id"}),indexes=@Index(name="idx_social_member",columnList="member_id"))
public class SocialAccount {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=20) private String provider;
    @Column(name="provider_user_id",nullable=false,length=255) private String providerUserId;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private Member member;
    @Column(nullable=false,updatable=false) private LocalDateTime createdAt;

    protected SocialAccount(){}
    public SocialAccount(String provider,String providerUserId,Member member){this.provider=provider;this.providerUserId=providerUserId;this.member=member;this.createdAt=LocalDateTime.now();}
    public Long getId(){return id;} public String getProvider(){return provider;} public String getProviderUserId(){return providerUserId;}
    public Member getMember(){return member;} public LocalDateTime getCreatedAt(){return createdAt;}
}
