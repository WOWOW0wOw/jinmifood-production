package com.jinmifood.shop.domain;

import jakarta.persistence.*;

@Entity @Table(name="order_items")
public class OrderItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private CustomerOrder order;
    @Column(nullable=false) private Long productId;
    @Column(nullable=false,length=120) private String productName;
    @Column(nullable=false) private long unitPrice;
    @Column(nullable=false) private int quantity;
    @Column(nullable=false) private long lineTotal;
    protected OrderItem() {}
    public OrderItem(Product p,int quantity){this.productId=p.getId();this.productName=p.getName();this.unitPrice=p.getPrice();this.quantity=quantity;this.lineTotal=Math.multiplyExact(unitPrice,quantity);}
    void attach(CustomerOrder v){order=v;} public Long getId(){return id;} public Long getProductId(){return productId;}
    public String getProductName(){return productName;} public long getUnitPrice(){return unitPrice;} public int getQuantity(){return quantity;} public long getLineTotal(){return lineTotal;}
}
