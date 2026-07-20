package com.jinmifood.shop.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="product_reviews",
    uniqueConstraints=@UniqueConstraint(name="uk_review_product_member",columnNames={"product_id","member_id"}),
    indexes=@Index(name="idx_review_product_created",columnList="product_id,created_at"))
public class ProductReview {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private Product product;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private Member member;
    @Column(nullable=false) private int rating;
    @Column(nullable=false,length=2000) private String content;
    @Column(nullable=false,updatable=false) private LocalDateTime createdAt;
    @Column(nullable=false) private LocalDateTime updatedAt;

    protected ProductReview() {}
    public ProductReview(Product product,Member member,int rating,String content){
        this.product=product;this.member=member;this.rating=rating;this.content=content;
    }
    @PrePersist void created(){createdAt=updatedAt=LocalDateTime.now();}
    @PreUpdate void updated(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;} public Product getProduct(){return product;} public Member getMember(){return member;}
    public int getRating(){return rating;} public String getContent(){return content;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
    public String getAuthorDisplayName(){return mask(member.getName());}
    private String mask(String name){if(name==null||name.isBlank())return "회원";return name.length()==1?name:name.substring(0,1)+"*".repeat(name.length()-1);}
}
