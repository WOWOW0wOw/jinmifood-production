package com.jinmifood.shop.domain;
public enum OrderStatus {
    PAYMENT_PENDING("결제 대기"), PAID("결제 완료"), PREPARING("상품 준비 중"),
    SHIPPED("배송 중"), DELIVERED("배송 완료"), CANCELLED("주문 취소");
    private final String label;
    OrderStatus(String label){this.label=label;}
    public String getLabel(){return label;}
    public boolean canTransitionTo(OrderStatus next){
        if(next==null||next==this)return false;
        return switch(this){
            case PAYMENT_PENDING -> next==PAID||next==CANCELLED;
            case PAID -> next==PREPARING||next==CANCELLED;
            case PREPARING -> next==SHIPPED||next==CANCELLED;
            case SHIPPED -> next==DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
