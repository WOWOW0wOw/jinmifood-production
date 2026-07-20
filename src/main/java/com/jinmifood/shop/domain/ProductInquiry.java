package com.jinmifood.shop.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="product_inquiries",indexes={
    @Index(name="idx_inquiry_product_created",columnList="product_id,created_at"),
    @Index(name="idx_inquiry_answered_created",columnList="answered_at,created_at")})
public class ProductInquiry {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private Product product;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private Member member;
    @Column(nullable=false,length=120) private String title;
    @Column(nullable=false,length=2000) private String content;
    @Column(length=3000) private String answer;
    @Column(length=120) private String answeredBy;
    @Column(nullable=false,updatable=false) private LocalDateTime createdAt;
    private LocalDateTime answeredAt;

    protected ProductInquiry() {}
    public ProductInquiry(Product product,Member member,String title,String content){this.product=product;this.member=member;this.title=title;this.content=content;}
    @PrePersist void created(){createdAt=LocalDateTime.now();}
    public void answer(String answer,String answeredBy){this.answer=answer;this.answeredBy=answeredBy;this.answeredAt=LocalDateTime.now();}
    public Long getId(){return id;} public Product getProduct(){return product;} public Member getMember(){return member;}
    public String getTitle(){return title;} public String getContent(){return content;} public String getAnswer(){return answer;}
    public String getAnsweredBy(){return answeredBy;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getAnsweredAt(){return answeredAt;}
    public boolean isAnswered(){return answer!=null&&!answer.isBlank();}
    public String getAuthorDisplayName(){String name=member.getName();if(name==null||name.isBlank())return "회원";return name.length()==1?name:name.substring(0,1)+"*".repeat(name.length()-1);}
}
