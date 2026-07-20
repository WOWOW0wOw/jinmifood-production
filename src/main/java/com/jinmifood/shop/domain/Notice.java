package com.jinmifood.shop.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="notices",indexes=@Index(name="idx_notice_pinned_created",columnList="pinned,created_at"))
public class Notice {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=160) private String title;
    @Column(nullable=false,length=10000) private String content;
    @Column(nullable=false) private boolean pinned;
    @Column(nullable=false,updatable=false) private LocalDateTime createdAt;
    @Column(nullable=false) private LocalDateTime updatedAt;

    protected Notice() {}
    public Notice(String title,String content,boolean pinned){update(title,content,pinned);}
    public void update(String title,String content,boolean pinned){this.title=title;this.content=content;this.pinned=pinned;}
    @PrePersist void created(){createdAt=updatedAt=LocalDateTime.now();}
    @PreUpdate void updated(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;} public String getTitle(){return title;} public String getContent(){return content;}
    public boolean isPinned(){return pinned;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
