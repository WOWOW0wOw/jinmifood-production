package com.jinmifood.shop.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 40)
    private String name;
    @Column(nullable = false, unique = true, length = 50)
    private String slug;
    @Column(nullable = false)
    private int displayOrder;

    protected Category() {}
    public Category(String name, String slug, int displayOrder) {
        this.name = name; this.slug = slug; this.displayOrder = displayOrder;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public int getDisplayOrder() { return displayOrder; }
}
