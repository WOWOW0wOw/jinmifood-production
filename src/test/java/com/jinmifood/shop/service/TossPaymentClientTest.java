package com.jinmifood.shop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinmifood.shop.config.properties.TossPaymentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.Base64;
import java.net.URI;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class TossPaymentClientTest {
    private MockRestServiceServer server;private TossPaymentClient client;

    @BeforeEach void setUp(){
        var builder=RestClient.builder();server=MockRestServiceServer.bindTo(builder).build();
        var properties=new TossPaymentProperties();
        properties.setApiBaseUrl(URI.create("https://api.tosspayments.com"));
        properties.setClientKey("test_ck_placeholder");
        properties.setSecretKey("test-secret");
        client=new TossPaymentClient(builder.baseUrl(properties.getApiBaseUrl().toString()).build(),new ObjectMapper(),properties);
    }

    @Test void confirmUsesBasicAuthAndRequiredBody(){
        String auth="Basic "+Base64.getEncoder().encodeToString("test-secret:".getBytes(StandardCharsets.UTF_8));
        server.expect(once(),requestTo("https://api.tosspayments.com/v1/payments/confirm"))
            .andExpect(method(HttpMethod.POST)).andExpect(header(HttpHeaders.AUTHORIZATION,auth))
            .andExpect(content().json("{\"paymentKey\":\"pay-key\",\"orderId\":\"order-1\",\"amount\":38500}"))
            .andRespond(withSuccess(payment("DONE"),MediaType.APPLICATION_JSON));
        var result=client.confirm("pay-key","order-1",38500);
        assertThat(result.status()).isEqualTo("DONE");assertThat(result.totalAmount()).isEqualTo(38500);
        assertThat(result.method()).isEqualTo("카드");assertThat(result.receiptUrl()).isEqualTo("https://receipt.test/1");
        server.verify();
    }

    @Test void findUsesPaymentLookupEndpoint(){
        server.expect(once(),requestTo("https://api.tosspayments.com/v1/payments/pay-key"))
            .andExpect(method(HttpMethod.GET)).andRespond(withSuccess(payment("DONE"),MediaType.APPLICATION_JSON));
        assertThat(client.find("pay-key").orderId()).isEqualTo("order-1");server.verify();
    }

    @Test void cancelSendsIdempotencyKeyAndReason(){
        server.expect(once(),requestTo("https://api.tosspayments.com/v1/payments/pay-key/cancel"))
            .andExpect(method(HttpMethod.POST)).andExpect(header("Idempotency-Key","cancel-order-1"))
            .andExpect(content().json("{\"cancelReason\":\"고객 요청\"}"))
            .andRespond(withSuccess(payment("CANCELED"),MediaType.APPLICATION_JSON));
        assertThat(client.cancel("pay-key","고객 요청","cancel-order-1").status()).isEqualTo("CANCELED");server.verify();
    }

    @Test void mapsTossErrorWithoutLeakingResponseDetails(){
        server.expect(once(),requestTo("https://api.tosspayments.com/v1/payments/confirm"))
            .andRespond(withBadRequest().contentType(MediaType.APPLICATION_JSON)
                .body("{\"code\":\"INVALID_REQUEST\",\"message\":\"잘못된 결제 요청입니다.\"}"));
        assertThatThrownBy(()->client.confirm("bad","order-1",1)).isInstanceOfSatisfying(PaymentException.class,e->{
            assertThat(e.getCode()).isEqualTo("INVALID_REQUEST");assertThat(e).hasMessage("잘못된 결제 요청입니다.");
        });server.verify();
    }

    @Test void mapsNetworkFailureToRetryablePaymentError(){
        server.expect(once(),requestTo("https://api.tosspayments.com/v1/payments/confirm"))
            .andRespond(withException(new IOException("temporary network failure")));
        assertThatThrownBy(()->client.confirm("pay-key","order-1",38500)).isInstanceOfSatisfying(PaymentException.class,e->{
            assertThat(e.getCode()).isEqualTo("PAYMENT_GATEWAY_UNAVAILABLE");assertThat(e).hasMessageContaining("다시 시도");
        });server.verify();
    }

    @Test void rejectsMalformedApprovalTimestampAsPaymentError(){
        server.expect(once(),requestTo("https://api.tosspayments.com/v1/payments/confirm"))
            .andRespond(withSuccess(payment("DONE").replace("2026-07-14T00:00:00+09:00","not-a-date"),MediaType.APPLICATION_JSON));
        assertThatThrownBy(()->client.confirm("pay-key","order-1",38500)).isInstanceOfSatisfying(PaymentException.class,e->
            assertThat(e.getCode()).isEqualTo("INVALID_PAYMENT_RESPONSE"));server.verify();
    }

    private String payment(String status){return """
        {"paymentKey":"pay-key","orderId":"order-1","totalAmount":38500,"status":"%s","method":"카드",
         "approvedAt":"2026-07-14T00:00:00+09:00","receipt":{"url":"https://receipt.test/1"}}
        """.formatted(status);}
}
