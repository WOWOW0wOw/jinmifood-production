package com.jinmifood.shop.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "products", indexes = @Index(name = "idx_product_active_category", columnList = "active,category_id"))
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Size(max = 120) @Column(nullable = false, length = 120)
    private String name;
    @NotBlank @Size(max = 80) @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "URL 식별자는 영문 소문자, 숫자, 하이픈만 사용할 수 있습니다")
    @Column(nullable = false, unique = true, length = 80)
    private String slug;
    @NotNull @PositiveOrZero @Column(nullable = false)
    private Integer price;
    @PositiveOrZero
    private Integer originalPrice;
    @NotNull @PositiveOrZero @Column(nullable = false)
    private Integer stock;
    @Size(max = 240)
    private String summary;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Size(max = 500)
    @Pattern(regexp = "^(?:|/images/[A-Za-z0-9._/-]+|/uploads/[A-Za-z0-9._-]+|https://[^\\s]+)$",
            message = "이미지 주소는 HTTPS URL 또는 쇼핑몰 내부 이미지 경로만 사용할 수 있습니다")
    private String imageUrl;
    @Size(max = 500)
    @Pattern(regexp = "^(?:|/images/[A-Za-z0-9._/-]+|/uploads/[A-Za-z0-9._-]+|https://[^\\s]+)$",
            message = "상세 이미지 주소는 HTTPS URL 또는 허용된 내부 이미지 경로만 사용할 수 있습니다")
    private String detailImageUrl;
    @Size(max = 100)
    private String origin;
    @Size(max = 120)
    private String manufacturer;
    @Size(max = 120)
    private String weight;
    @Size(max = 160)
    private String shelfLife;
    @Size(max = 200)
    private String storageMethod;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private boolean featured;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Category category;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Product() {}
    public Product(String name, String slug, int price, Integer originalPrice, int stock, String summary,
                   String description, String imageUrl, boolean featured, Category category) {
        this.name=name; this.slug=slug; this.price=price; this.originalPrice=originalPrice; this.stock=stock;
        this.summary=summary; this.description=description; this.imageUrl=imageUrl; this.featured=featured; this.category=category;
    }
    @PrePersist void created() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void updated() { updatedAt = LocalDateTime.now(); }
    public void decreaseStock(int quantity) {
        if (quantity < 1 || stock < quantity) throw new IllegalStateException(name + " 재고가 부족합니다.");
        stock -= quantity;
    }
    public void increaseStock(int quantity) { if (quantity > 0) stock += quantity; }
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getName(){return name;} public void setName(String v){name=v;}
    public String getSlug(){return slug;} public void setSlug(String v){slug=v;}
    public Integer getPrice(){return price;} public void setPrice(Integer v){price=v;}
    public Integer getOriginalPrice(){return originalPrice;} public void setOriginalPrice(Integer v){originalPrice=v;}
    public Integer getStock(){return stock;} public void setStock(Integer v){stock=v;}
    public String getSummary(){return summary;} public void setSummary(String v){summary=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getImageUrl(){return imageUrl;} public void setImageUrl(String v){imageUrl=v;}
    public String getDetailImageUrl(){return detailImageUrl;} public void setDetailImageUrl(String v){detailImageUrl=v;}
    public String getOrigin(){return origin;} public void setOrigin(String v){origin=v;}
    public String getManufacturer(){return manufacturer;} public void setManufacturer(String v){manufacturer=v;}
    public String getWeight(){return weight;} public void setWeight(String v){weight=v;}
    public String getShelfLife(){return shelfLife;} public void setShelfLife(String v){shelfLife=v;}
    public String getStorageMethod(){return storageMethod;} public void setStorageMethod(String v){storageMethod=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public boolean isFeatured(){return featured;} public void setFeatured(boolean v){featured=v;}
    public Category getCategory(){return category;} public void setCategory(Category v){category=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
