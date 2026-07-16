package com.jinmifood.shop.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="members", indexes=@Index(name="idx_member_email", columnList="email", unique=true))
public class Member {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=120) private String email;
    @Column(nullable=false, length=100) private String passwordHash;
    @Column(nullable=false, length=40) private String name;
    @Column(nullable=false, length=20) private String phone;
    @Column(nullable=false) private int points;
    @Column(nullable=false) private boolean admin;
    @Column(nullable=false) private boolean active=true;
    @Column(length=10) private String postalCode;
    @Column(length=200) private String address;
    @Column(length=200) private String addressDetail;
    @Column(nullable=false, updatable=false) private LocalDateTime createdAt;
    private LocalDateTime withdrawnAt;

    protected Member() {}
    public Member(String email,String passwordHash,String name,String phone){
        this.email=email.toLowerCase();this.passwordHash=passwordHash;this.name=name;this.phone=phone;
        this.createdAt=LocalDateTime.now();
    }
    public void usePoints(int amount){if(amount<0||amount>points)throw new IllegalStateException("사용 가능한 포인트를 확인해 주세요.");points-=amount;}
    public void addPoints(int amount){if(amount>0)points+=amount;}
    public void grantAdmin(){admin=true;}
    public void revokeAdmin(){admin=false;}
    public void activate(){active=true;}
    public void deactivate(){active=false;}
    public void adjustPoints(int delta){
        long adjusted=(long)points+delta;
        if(adjusted<0||adjusted>Integer.MAX_VALUE)throw new IllegalStateException("포인트 잔액을 확인해 주세요.");
        points=(int)adjusted;
    }
    public void changePassword(String passwordHash){this.passwordHash=passwordHash;}
    public void verifyPhone(String phone){
        if(phone==null||phone.isBlank())throw new IllegalArgumentException("휴대전화번호를 입력해 주세요.");
        this.phone=phone;
    }
    public void withdraw(String anonymizedEmail,String anonymizedPasswordHash,LocalDateTime withdrawnAt){
        if(this.withdrawnAt!=null)throw new IllegalStateException("이미 탈퇴한 회원입니다.");
        this.email=anonymizedEmail;this.passwordHash=anonymizedPasswordHash;this.name="탈퇴 회원";this.phone="";
        this.points=0;this.admin=false;this.active=false;this.postalCode=null;this.address=null;this.addressDetail=null;
        this.withdrawnAt=withdrawnAt;
    }
    public void updateDefaultAddress(String postalCode,String address,String addressDetail){
        this.postalCode=clean(postalCode);this.address=clean(address);this.addressDetail=clean(addressDetail);
    }
    private String clean(String value){return value==null||value.isBlank()?null:value.trim();}
    public void removePoints(int amount){
        if(amount<0||amount>points)throw new IllegalStateException("이미 사용한 적립 포인트가 있어 주문을 취소할 수 없습니다.");
        points-=amount;
    }
    public Long getId(){return id;} public String getEmail(){return email;} public String getPasswordHash(){return passwordHash;}
    public String getName(){return name;} public String getPhone(){return phone;} public int getPoints(){return points;}
    public boolean isAdmin(){return admin;}
    public boolean isActive(){return active;}
    public boolean isWithdrawn(){return withdrawnAt!=null;}
    public String getPostalCode(){return postalCode;} public String getAddress(){return address;} public String getAddressDetail(){return addressDetail;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public LocalDateTime getWithdrawnAt(){return withdrawnAt;}
}
