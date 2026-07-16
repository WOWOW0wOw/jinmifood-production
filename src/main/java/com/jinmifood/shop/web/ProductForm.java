package com.jinmifood.shop.web;

import com.jinmifood.shop.domain.Product;
import jakarta.validation.constraints.*;

public class ProductForm {
    private Long id;
    @NotBlank @Size(max=120) private String name;
    @NotBlank @Size(max=80) @Pattern(regexp="^[a-z0-9]+(?:-[a-z0-9]+)*$",message="URL 식별자는 영문 소문자, 숫자, 하이픈만 사용할 수 있습니다") private String slug;
    @NotNull private Long categoryId;
    @NotNull @PositiveOrZero @Max(100000000) private Integer price;
    @PositiveOrZero @Max(100000000) private Integer originalPrice;
    @NotNull @PositiveOrZero @Max(1000000) private Integer stock;
    @Size(max=240) private String summary;
    private String description;
    @Size(max=100) private String origin; @Size(max=120) private String manufacturer; @Size(max=120) private String weight;
    @Size(max=160) private String shelfLife; @Size(max=200) private String storageMethod;
    private boolean active=true; private boolean featured;

    public static ProductForm from(Product p){
        var f=new ProductForm();f.id=p.getId();f.name=p.getName();f.slug=p.getSlug();f.categoryId=p.getCategory().getId();f.price=p.getPrice();f.originalPrice=p.getOriginalPrice();f.stock=p.getStock();f.summary=p.getSummary();f.description=p.getDescription();f.origin=p.getOrigin();f.manufacturer=p.getManufacturer();f.weight=p.getWeight();f.shelfLife=p.getShelfLife();f.storageMethod=p.getStorageMethod();f.active=p.isActive();f.featured=p.isFeatured();return f;
    }
    public void applyTo(Product p){p.setName(name.trim());p.setSlug(slug.trim());p.setPrice(price);p.setOriginalPrice(originalPrice);p.setStock(stock);p.setSummary(summary);p.setDescription(description);p.setOrigin(origin);p.setManufacturer(manufacturer);p.setWeight(weight);p.setShelfLife(shelfLife);p.setStorageMethod(storageMethod);p.setActive(active);p.setFeatured(featured);}
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getName(){return name;} public void setName(String v){name=v;} public String getSlug(){return slug;} public void setSlug(String v){slug=v;}
    public Long getCategoryId(){return categoryId;} public void setCategoryId(Long v){categoryId=v;} public Integer getPrice(){return price;} public void setPrice(Integer v){price=v;} public Integer getOriginalPrice(){return originalPrice;} public void setOriginalPrice(Integer v){originalPrice=v;}
    public Integer getStock(){return stock;} public void setStock(Integer v){stock=v;} public String getSummary(){return summary;} public void setSummary(String v){summary=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getOrigin(){return origin;} public void setOrigin(String v){origin=v;} public String getManufacturer(){return manufacturer;} public void setManufacturer(String v){manufacturer=v;}
    public String getWeight(){return weight;} public void setWeight(String v){weight=v;} public String getShelfLife(){return shelfLife;} public void setShelfLife(String v){shelfLife=v;} public String getStorageMethod(){return storageMethod;} public void setStorageMethod(String v){storageMethod=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;} public boolean isFeatured(){return featured;} public void setFeatured(boolean v){featured=v;}
}
