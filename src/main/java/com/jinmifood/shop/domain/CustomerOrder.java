package com.jinmifood.shop.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name="customer_orders", indexes={@Index(name="idx_order_number",columnList="orderNumber",unique=true),@Index(name="idx_order_created",columnList="createdAt"),@Index(name="idx_order_payment_key",columnList="paymentKey",unique=true)})
public class CustomerOrder {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,length=24) private String orderNumber;
    @Column(nullable=false,length=40) private String customerName;
    @Column(nullable=false,length=20) private String phone;
    @Column(nullable=false,length=120) private String email;
    @Column(nullable=false,length=10) private String postalCode;
    @Column(nullable=false,length=200) private String address;
    @Column(length=200) private String addressDetail;
    @Column(length=200) private String deliveryMemo;
    @Column(nullable=false) private long subtotal;
    @Column(nullable=false) private long shippingFee;
    @Column(nullable=false) private long totalAmount;
    @ManyToOne(fetch=FetchType.LAZY) private Member member;
    @Column(nullable=false,columnDefinition="integer default 0") private int pointsUsed;
    @Column(nullable=false,columnDefinition="integer default 0") private int pointsEarned;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private OrderStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PaymentStatus paymentStatus;
    @Column(unique=true,length=200) private String paymentKey;
    @Column(length=40) private String paymentMethod;
    private LocalDateTime paidAt;
    @Column(length=500) private String receiptUrl;
    @Column(length=80) private String paymentFailureCode;
    @Column(length=200) private String paymentFailureMessage;
    @Column(nullable=false,updatable=false) private LocalDateTime createdAt;
    @OneToMany(mappedBy="order",cascade=CascadeType.ALL,orphanRemoval=true) private List<OrderItem> items=new ArrayList<>();
    protected CustomerOrder() {}
    public CustomerOrder(String number,String name,String phone,String email,String postal,String address,String detail,String memo,long subtotal,long shipping,Member member,int pointsUsed){
        this.orderNumber=number;this.customerName=name;this.phone=phone;this.email=email;this.postalCode=postal;this.address=address;
        this.addressDetail=detail;this.deliveryMemo=memo;this.subtotal=subtotal;this.shippingFee=shipping;this.member=member;this.pointsUsed=pointsUsed;this.totalAmount=subtotal+shipping-pointsUsed;
        this.status=OrderStatus.PAYMENT_PENDING;this.paymentStatus=PaymentStatus.READY;this.createdAt=LocalDateTime.now();
    }
    public void addItem(OrderItem item){items.add(item);item.attach(this);} public void changeStatus(OrderStatus v){status=v;}
    public void awardPoints(int amount){pointsEarned=Math.max(0,amount);} public int revokePoints(){int value=pointsEarned;pointsEarned=0;return value;}
    public void markPaymentPaid(String key,String method,LocalDateTime approvedAt,String receipt){
        if(key==null||key.isBlank())throw new IllegalArgumentException("결제 키가 필요합니다.");
        if(getPaymentStatus()==PaymentStatus.PAID&&!Objects.equals(paymentKey,key))throw new IllegalStateException("이미 다른 결제로 승인된 주문입니다.");
        paymentStatus=PaymentStatus.PAID;paymentKey=key;paymentMethod=method;paidAt=approvedAt;receiptUrl=receipt;
        paymentFailureCode=null;paymentFailureMessage=null;
    }
    public void markPaymentCancelled(){paymentStatus=PaymentStatus.CANCELLED;}
    public void markPaymentFailed(String code,String message){
        if(getPaymentStatus()==PaymentStatus.PAID)return;
        paymentStatus=PaymentStatus.FAILED;paymentFailureCode=truncate(code,80);paymentFailureMessage=truncate(message,200);
    }
    public void preparePaymentRetry(){if(getPaymentStatus()==PaymentStatus.FAILED){paymentStatus=PaymentStatus.READY;paymentFailureCode=null;paymentFailureMessage=null;}}
    public void repairLegacyPaymentStatus(){if(paymentStatus==null)paymentStatus=derivedPaymentStatus();}
    private String truncate(String value,int max){return value==null?null:value.substring(0,Math.min(value.length(),max));}
    public Long getId(){return id;} public String getOrderNumber(){return orderNumber;} public String getCustomerName(){return customerName;}
    public String getPhone(){return phone;} public String getEmail(){return email;} public String getPostalCode(){return postalCode;}
    public String getAddress(){return address;} public String getAddressDetail(){return addressDetail;} public String getDeliveryMemo(){return deliveryMemo;}
    public long getSubtotal(){return subtotal;} public long getShippingFee(){return shippingFee;} public long getTotalAmount(){return totalAmount;}
    public Member getMember(){return member;} public int getPointsUsed(){return pointsUsed;} public int getPointsEarned(){return pointsEarned;}
    public OrderStatus getStatus(){return status;} public LocalDateTime getCreatedAt(){return createdAt;} public List<OrderItem> getItems(){return items;}
    public PaymentStatus getPaymentStatus(){return paymentStatus==null?derivedPaymentStatus():paymentStatus;} public String getPaymentKey(){return paymentKey;} public String getPaymentMethod(){return paymentMethod;}
    public LocalDateTime getPaidAt(){return paidAt;} public String getReceiptUrl(){return receiptUrl;} public String getPaymentFailureCode(){return paymentFailureCode;}
    public String getPaymentFailureMessage(){return paymentFailureMessage;}
    private PaymentStatus derivedPaymentStatus(){
        if(status==OrderStatus.CANCELLED)return PaymentStatus.CANCELLED;
        if(status==OrderStatus.PAID||status==OrderStatus.PREPARING||status==OrderStatus.SHIPPED||status==OrderStatus.DELIVERED)return PaymentStatus.PAID;
        return PaymentStatus.READY;
    }
}
