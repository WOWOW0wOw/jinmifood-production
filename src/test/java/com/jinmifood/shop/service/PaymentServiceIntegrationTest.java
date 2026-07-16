package com.jinmifood.shop.service;

import com.jinmifood.shop.domain.*;
import com.jinmifood.shop.repository.ProductRepository;
import com.jinmifood.shop.web.Cart;
import com.jinmifood.shop.web.CheckoutForm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentServiceIntegrationTest {
    @Autowired PaymentService payments;
    @Autowired ShopService shop;
    @Autowired ProductRepository products;
    @MockitoBean PaymentGateway gateway;

    @Test void confirmsOnceAndPersistsVerifiedPayment(){
        var order=order();var result=result(order,"DONE");
        when(gateway.confirm("payment-key",order.getOrderNumber(),order.getTotalAmount())).thenReturn(result);
        var paid=payments.confirm(order.getOrderNumber(),"payment-key",order.getTotalAmount());
        var duplicate=payments.confirm(order.getOrderNumber(),"payment-key",order.getTotalAmount());
        assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paid.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(paid.getPaymentKey()).isEqualTo("payment-key");
        assertThat(duplicate).isSameAs(paid);
        verify(gateway,times(1)).confirm("payment-key",order.getOrderNumber(),order.getTotalAmount());
    }

    @Test void rejectsClientAmountBeforeCallingToss(){
        var order=order();
        assertThatThrownBy(()->payments.confirm(order.getOrderNumber(),"payment-key",order.getTotalAmount()-1))
            .isInstanceOf(PaymentException.class).hasMessageContaining("금액");
        verifyNoInteractions(gateway);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }

    @Test void adminCancellationCancelsTossAndRestoresStock(){
        var product=products.findByActiveTrueOrderByCreatedAtDesc().getFirst();int initialStock=product.getStock();
        var order=order(product);when(gateway.confirm(anyString(),anyString(),anyLong())).thenReturn(result(order,"DONE"));
        payments.confirm(order.getOrderNumber(),"payment-key",order.getTotalAmount());
        when(gateway.cancel("payment-key","테스트 취소","cancel-order-"+order.getId())).thenReturn(result(order,"CANCELED"));
        payments.cancel(order.getId(),"테스트 취소");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(initialStock);
    }

    @Test void webhookRequeriesTossBeforeMarkingOrderPaid(){
        var order=order();when(gateway.find("payment-key")).thenReturn(result(order,"DONE"));
        payments.syncWebhook("payment-key",order.getOrderNumber());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        verify(gateway).find("payment-key");
    }

    @Test void rejectsTossResponseThatDoesNotMatchOrder(){
        var order=order();var mismatched=new PaymentGateway.PaymentResult("payment-key","another-order",order.getTotalAmount(),"DONE","카드",LocalDateTime.now(),null);
        when(gateway.confirm("payment-key",order.getOrderNumber(),order.getTotalAmount())).thenReturn(mismatched);
        assertThatThrownBy(()->payments.confirm(order.getOrderNumber(),"payment-key",order.getTotalAmount()))
            .isInstanceOf(PaymentException.class).hasMessageContaining("일치하지 않습니다");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }

    @Test void failedPaymentCanBePreparedForRetry(){
        var order=order();payments.recordFailure(order.getOrderNumber(),"REJECT_CARD_COMPANY","카드 승인 거절");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        payments.prepare(order.getOrderNumber());
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(order.getPaymentFailureCode()).isNull();
    }

    private CustomerOrder order(){return order(products.findByActiveTrueOrderByCreatedAtDesc().getFirst());}
    private CustomerOrder order(Product product){
        var cart=new Cart();cart.add(product.getId(),1);var form=new CheckoutForm();
        form.setCustomerName("결제 테스트");form.setPhone("010-1234-5678");form.setEmail("pay@example.com");
        form.setPostalCode("07200");form.setAddress("서울시 영등포구");form.setAgreed(true);
        return shop.placeOrder(cart,form,null);
    }
    private PaymentGateway.PaymentResult result(CustomerOrder order,String status){
        return new PaymentGateway.PaymentResult("payment-key",order.getOrderNumber(),order.getTotalAmount(),status,"카드",LocalDateTime.now(),"https://example.test/receipt");
    }
}
