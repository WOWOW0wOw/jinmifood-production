package com.jinmifood.shop.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="sms_verifications",indexes={
        @Index(name="idx_sms_phone_purpose_created",columnList="phone,purpose,created_at"),
        @Index(name="idx_sms_expires",columnList="expires_at")})
public class SmsVerification {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=20) private String phone;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private VerificationPurpose purpose;
    @Column(nullable=false,length=100) private String codeHash;
    @Column(nullable=false) private LocalDateTime expiresAt;
    @Column(nullable=false) private int attempts;
    private LocalDateTime verifiedAt;
    @Column(nullable=false,updatable=false) private LocalDateTime createdAt;

    protected SmsVerification(){}
    public SmsVerification(String phone,VerificationPurpose purpose,String codeHash,LocalDateTime expiresAt){
        this.phone=phone;this.purpose=purpose;this.codeHash=codeHash;this.expiresAt=expiresAt;this.createdAt=LocalDateTime.now();
    }
    public void recordFailure(){attempts++;}
    public void markVerified(){verifiedAt=LocalDateTime.now();}
    public boolean canTry(LocalDateTime now){return verifiedAt==null&&attempts<5&&expiresAt.isAfter(now);}
    public Long getId(){return id;} public String getPhone(){return phone;} public VerificationPurpose getPurpose(){return purpose;}
    public String getCodeHash(){return codeHash;} public LocalDateTime getExpiresAt(){return expiresAt;} public int getAttempts(){return attempts;}
    public LocalDateTime getVerifiedAt(){return verifiedAt;} public LocalDateTime getCreatedAt(){return createdAt;}
}
