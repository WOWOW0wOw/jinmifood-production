package com.jinmifood.shop.service;

import com.jinmifood.shop.domain.*;
import com.jinmifood.shop.repository.CustomerOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class PaymentService {
    private final CustomerOrderRepository orders;private final PaymentGateway gateway;private final ShopService shop;
    public PaymentService(CustomerOrderRepository orders,PaymentGateway gateway,ShopService shop){this.orders=orders;this.gateway=gateway;this.shop=shop;}

    @Transactional
    public CustomerOrder prepare(String orderNumber){
        var order=locked(orderNumber);
        if(order.getPaymentStatus()==PaymentStatus.PAID)return order;
        if(order.getStatus()!=OrderStatus.PAYMENT_PENDING)throw new PaymentException("INVALID_ORDER_STATUS","결제할 수 없는 주문 상태입니다.");
        order.preparePaymentRetry();return order;
    }

    @Transactional
    public CustomerOrder confirm(String orderNumber,String paymentKey,long amount){
        var order=locked(orderNumber);
        if(amount!=order.getTotalAmount())throw new PaymentException("AMOUNT_MISMATCH","결제 금액이 주문 금액과 일치하지 않습니다.");
        if(order.getPaymentStatus()==PaymentStatus.PAID){
            if(Objects.equals(order.getPaymentKey(),paymentKey))return order;
            throw new PaymentException("ALREADY_PAID","이미 다른 결제로 승인된 주문입니다.");
        }
        if(order.getStatus()!=OrderStatus.PAYMENT_PENDING)throw new PaymentException("INVALID_ORDER_STATUS","결제할 수 없는 주문 상태입니다.");
        var result=gateway.confirm(paymentKey,orderNumber,amount);validate(order,result,"DONE");
        applyPaid(order,result);return order;
    }

    @Transactional
    public CustomerOrder cancel(Long orderId,String reason){
        var order=orders.findByIdForUpdate(orderId).orElseThrow(()->new NoSuchElementException("주문을 찾을 수 없습니다."));
        if(order.getPaymentStatus()!=PaymentStatus.PAID||order.getPaymentKey()==null)throw new PaymentException("NOT_PAID","승인된 결제가 없습니다.");
        var result=gateway.cancel(order.getPaymentKey(),safeReason(reason),"cancel-order-"+order.getId());
        if(!"CANCELED".equals(result.status()))throw new PaymentException("CANCEL_NOT_COMPLETED","토스 결제가 취소 완료 상태가 아닙니다.");
        shop.changeOrderStatus(order.getId(),OrderStatus.CANCELLED);order.markPaymentCancelled();return order;
    }

    @Transactional
    public void recordFailure(String orderNumber,String code,String message){
        orders.findByOrderNumberForUpdate(orderNumber).ifPresent(order->order.markPaymentFailed(code,message));
    }

    @Transactional
    public void syncWebhook(String paymentKey,String orderNumber){
        var order=locked(orderNumber);var result=gateway.find(paymentKey);validateIdentity(order,result);
        if("DONE".equals(result.status())&&order.getPaymentStatus()!=PaymentStatus.PAID){applyPaid(order,result);}
        else if("CANCELED".equals(result.status())&&order.getStatus()!=OrderStatus.CANCELLED){
            if(order.getStatus().canTransitionTo(OrderStatus.CANCELLED))shop.changeOrderStatus(order.getId(),OrderStatus.CANCELLED);
            order.markPaymentCancelled();
        }else if(("ABORTED".equals(result.status())||"EXPIRED".equals(result.status()))&&order.getPaymentStatus()!=PaymentStatus.PAID){
            order.markPaymentFailed(result.status(),"결제 인증이 만료되었거나 중단되었습니다.");
        }
    }

    private void applyPaid(CustomerOrder order,PaymentGateway.PaymentResult result){
        order.markPaymentPaid(result.paymentKey(),result.method(),result.approvedAt(),result.receiptUrl());
        shop.changeOrderStatus(order.getId(),OrderStatus.PAID);
    }
    private void validate(CustomerOrder order,PaymentGateway.PaymentResult result,String expectedStatus){
        validateIdentity(order,result);
        if(!expectedStatus.equals(result.status()))throw new PaymentException("INVALID_PAYMENT_STATUS","토스 결제가 승인 완료 상태가 아닙니다.");
    }
    private void validateIdentity(CustomerOrder order,PaymentGateway.PaymentResult result){
        if(result==null||!Objects.equals(order.getOrderNumber(),result.orderId())||order.getTotalAmount()!=result.totalAmount())
            throw new PaymentException("PAYMENT_MISMATCH","토스 결제 정보와 주문 정보가 일치하지 않습니다.");
    }
    private CustomerOrder locked(String orderNumber){return orders.findByOrderNumberForUpdate(orderNumber).orElseThrow(()->new NoSuchElementException("주문을 찾을 수 없습니다."));}
    private String safeReason(String reason){String value=reason==null?"고객 요청에 따른 주문 취소":reason.trim();return value.isBlank()?"고객 요청에 따른 주문 취소":value.substring(0,Math.min(value.length(),200));}
}
