package com.jinmifood.shop.service;

import com.jinmifood.shop.domain.*;
import com.jinmifood.shop.repository.ProductRepository;
import com.jinmifood.shop.web.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CommerceReliabilityMatrixTest {
    @Autowired ShopService shop;@Autowired PaymentService payments;@Autowired ProductRepository products;@Autowired MockMvc mvc;
    @MockitoBean PaymentGateway gateway;

    @ParameterizedTest(name="주문 생성 변형 {0}")
    @ValueSource(ints={1,2,3,4,5,6,7,8,9,10})
    void placesTenDifferentOrders(int variant){
        var product=products.findByActiveTrueOrderByCreatedAtDesc().get(variant%products.findByActiveTrueOrderByCreatedAtDesc().size());
        var order=order(product,"order"+variant+"@example.com","요청사항 "+variant);
        assertThat(order.getOrderNumber()).startsWith("JM");assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotalAmount()).isEqualTo(product.getPrice()+order.getShippingFee());
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.READY);
    }

    @ParameterizedTest(name="정상 결제 승인 변형 {0}")
    @ValueSource(ints={11,12,13,14,15,16,17,18,19,20})
    void confirmsTenDifferentPayments(int variant){
        var order=order(products.findByActiveTrueOrderByCreatedAtDesc().getFirst(),"paid"+variant+"@example.com",null);String key="payment-key-"+variant;
        when(gateway.confirm(key,order.getOrderNumber(),order.getTotalAmount())).thenReturn(result(order,key,"DONE"));
        assertThat(payments.confirm(order.getOrderNumber(),key,order.getTotalAmount()).getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        verify(gateway).confirm(key,order.getOrderNumber(),order.getTotalAmount());
    }

    @ParameterizedTest(name="금액 변조 차단 {0}")
    @ValueSource(longs={-10000,-1000,-100,-10,-1,1,10,100,1000,10000})
    void blocksTenTamperedAmounts(long delta){
        var order=order(products.findByActiveTrueOrderByCreatedAtDesc().getFirst(),"amount"+delta+"@example.com",null);
        assertThatThrownBy(()->payments.confirm(order.getOrderNumber(),"tampered",order.getTotalAmount()+delta))
            .isInstanceOf(PaymentException.class).hasMessageContaining("금액");
        verifyNoInteractions(gateway);
    }

    @ParameterizedTest(name="결제 실패 복구 {0}")
    @ValueSource(strings={"PAY_PROCESS_CANCELED","PAY_PROCESS_ABORTED","REJECT_CARD_COMPANY","INVALID_CARD_EXPIRATION","INVALID_STOPPED_CARD","EXCEED_MAX_DAILY_PAYMENT_COUNT","NOT_SUPPORTED_INSTALLMENT_PLAN_CARD_OR_MERCHANT","INVALID_CARD_INSTALLMENT_PLAN","INVALID_REJECT_CARD","UNKNOWN_PAYMENT_ERROR"})
    void retriesTenDifferentPaymentFailures(String code){
        var order=order(products.findByActiveTrueOrderByCreatedAtDesc().getFirst(),code.toLowerCase()+"@example.com",null);
        payments.recordFailure(order.getOrderNumber(),code,"테스트 실패");assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        payments.prepare(order.getOrderNumber());assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.READY);
    }

    @ParameterizedTest(name="잘못된 결제 세션 {0}")
    @ValueSource(strings={"JM-NO-SESSION-01","JM-NO-SESSION-02","JM-NO-SESSION-03","JM-NO-SESSION-04","JM-NO-SESSION-05","JM-NO-SESSION-06","JM-NO-SESSION-07","JM-NO-SESSION-08","JM-NO-SESSION-09","JM-NO-SESSION-10"})
    void handlesTenInvalidPaymentSessionsWithout500(String orderNumber)throws Exception{
        mvc.perform(get("/payments/toss/request/{orderNumber}",orderNumber)).andExpect(status().isOk())
            .andExpect(view().name("store/payment-fail")).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("일시적인 오류가 발생했습니다"))));
    }

    @ParameterizedTest(name="외부 복귀주소 차단 {0}")
    @ValueSource(strings={"//evil.example","https://evil.example","http://evil.example","javascript:alert(1)","/\\evil","///evil.example","data:text/html,bad","%2f%2fevil.example","https:%2f%2fevil.example","\\\\evil.example"})
    void blocksTenUnsafeCartReturnUrls(String returnUrl)throws Exception{
        var product=products.findByActiveTrueOrderByCreatedAtDesc().getFirst();
        mvc.perform(post("/cart/add").with(csrf()).param("productId",product.getId().toString()).param("quantity","1").param("returnUrl",returnUrl))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/cart"));
    }

    @ParameterizedTest(name="관리자 비로그인 차단 {0}")
    @MethodSource("adminUrls")
    void protectsTenAdminUrls(String url)throws Exception{
        mvc.perform(get(url)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
    }

    @ParameterizedTest(name="비정상 웹훅 처리 {index}")
    @MethodSource("invalidWebhooks")
    void handlesTenInvalidWebhooksWithout500(String body,int expectedStatus)throws Exception{
        mvc.perform(post("/webhooks/toss").contentType("application/json").content(body)).andExpect(status().is(expectedStatus));
    }

    @Test
    void paymentRedirectDoesNotTrustTheHostHeader() throws Exception {
        var order=order(products.findByActiveTrueOrderByCreatedAtDesc().getFirst(),"host-header@example.com",null);
        var session=new MockHttpSession();
        session.setAttribute("pendingPaymentOrderNumber",order.getOrderNumber());

        mvc.perform(get("/payments/toss/request/{orderNumber}",order.getOrderNumber())
                .session(session).header("Host","evil.example"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("http://localhost:8080/payments/toss/success")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("evil.example"))));
    }

    @Test
    void tossWebhookRejectsNonJsonContent() throws Exception {
        mvc.perform(post("/webhooks/toss").contentType("text/plain").content("{}"))
            .andExpect(status().isUnsupportedMediaType());
    }

    @ParameterizedTest(name="결제 승인 오류 화면 복구 {0}")
    @ValueSource(strings={"PAYMENT_GATEWAY_UNAVAILABLE","INVALID_REQUEST","UNAUTHORIZED_KEY","NOT_FOUND_PAYMENT_SESSION","REJECT_CARD_COMPANY","FORBIDDEN_REQUEST","ALREADY_PROCESSED_PAYMENT","EXCEED_MAX_CARD_INSTALLMENT_PLAN","INVALID_API_KEY","UNKNOWN_PAYMENT_ERROR"})
    void handlesTenApprovalErrorsWithout500(String code)throws Exception{
        var order=order(products.findByActiveTrueOrderByCreatedAtDesc().getFirst(),"approval-"+code.toLowerCase()+"@example.com",null);String key="failed-"+code;
        when(gateway.confirm(key,order.getOrderNumber(),order.getTotalAmount())).thenThrow(new PaymentException(code,"결제를 처리하지 못했습니다. 다시 시도해 주세요."));
        var session=new MockHttpSession();session.setAttribute("pendingPaymentOrderNumber",order.getOrderNumber());
        mvc.perform(get("/payments/toss/success").session(session).param("paymentKey",key).param("orderId",order.getOrderNumber()).param("amount",Long.toString(order.getTotalAmount())))
            .andExpect(status().isOk()).andExpect(view().name("store/payment-fail"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("일시적인 오류가 발생했습니다"))));
    }

    @ParameterizedTest(name="관리자 결제취소 오류 복구 {0}")
    @ValueSource(strings={"PAYMENT_GATEWAY_UNAVAILABLE","INVALID_REQUEST","UNAUTHORIZED_KEY","NOT_FOUND_PAYMENT","FORBIDDEN_REQUEST","ALREADY_CANCELED_PAYMENT","NOT_CANCELABLE_AMOUNT","INVALID_REFUND_ACCOUNT_INFO","EXCEED_CANCEL_AMOUNT_DISCOUNT_AMOUNT","UNKNOWN_PAYMENT_ERROR"})
    void handlesTenAdminCancellationErrorsWithout500(String code)throws Exception{
        var order=order(products.findByActiveTrueOrderByCreatedAtDesc().getFirst(),"cancel-"+code.toLowerCase()+"@example.com",null);String key="paid-"+code;
        when(gateway.confirm(key,order.getOrderNumber(),order.getTotalAmount())).thenReturn(result(order,key,"DONE"));payments.confirm(order.getOrderNumber(),key,order.getTotalAmount());
        when(gateway.cancel(key,"관리자 테스트 취소","cancel-order-"+order.getId())).thenThrow(new PaymentException(code,"토스 취소 요청을 처리하지 못했습니다."));
        mvc.perform(post("/admin/orders/{id}/payment/cancel",order.getId()).with(user("admin").roles("ADMIN")).with(csrf()).param("reason","관리자 테스트 취소"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/orders"))
            .andExpect(flash().attribute("message",org.hamcrest.Matchers.containsString("결제 취소 실패")));
    }

    static Stream<String> adminUrls(){return Stream.of("/admin","/admin/","/admin/products","/admin/products?active=true","/admin/products/new","/admin/products/1/edit","/admin/orders","/admin/orders?page=1","/admin?tab=orders","/admin?refresh=1");}
    static Stream<Arguments> invalidWebhooks(){return Stream.of(
        Arguments.of("{}",200),Arguments.of("{\"eventType\":\"UNKNOWN\"}",200),Arguments.of("{\"eventType\":\"PAYMENT_STATUS_CHANGED\",\"data\":{}}",400),
        Arguments.of("{\"eventType\":\"PAYMENT_STATUS_CHANGED\",\"data\":{\"paymentKey\":\"\"}}",400),Arguments.of("{\"eventType\":\"PAYMENT_STATUS_CHANGED\",\"data\":{\"orderId\":\"missing\"}}",400),
        Arguments.of("{\"eventType\":\"CANCEL_STATUS_CHANGED\",\"data\":{}}",200),Arguments.of("{\"eventType\":\"DEPOSIT_CALLBACK\"}",200),Arguments.of("{\"eventType\":null}",200),
        Arguments.of("{\"eventType\":\"PAYMENT_STATUS_CHANGED\",\"data\":{\"paymentKey\":\"unknown-1\",\"orderId\":\"unknown-1\"}}",400),
        Arguments.of("{\"eventType\":\"PAYMENT_STATUS_CHANGED\",\"data\":{\"paymentKey\":\"unknown-2\",\"orderId\":\"unknown-2\"}}",400));}

    private CustomerOrder order(Product product,String email,String memo){
        var cart=new Cart();cart.add(product.getId(),1);var form=new CheckoutForm();form.setCustomerName("반복 테스트");form.setPhone("010-1234-5678");
        form.setEmail(email);form.setPostalCode("07200");form.setAddress("서울시 영등포구");form.setDeliveryMemo(memo);form.setAgreed(true);return shop.placeOrder(cart,form,null);
    }
    private PaymentGateway.PaymentResult result(CustomerOrder order,String key,String status){return new PaymentGateway.PaymentResult(key,order.getOrderNumber(),order.getTotalAmount(),status,"카드",LocalDateTime.now(),null);}
}
