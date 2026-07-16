package com.jinmifood.shop.service;

import java.time.LocalDateTime;

public interface PaymentGateway {
    PaymentResult confirm(String paymentKey,String orderId,long amount);
    PaymentResult find(String paymentKey);
    PaymentResult cancel(String paymentKey,String reason,String idempotencyKey);

    record PaymentResult(String paymentKey,String orderId,long totalAmount,String status,String method,
                         LocalDateTime approvedAt,String receiptUrl) {}
}
