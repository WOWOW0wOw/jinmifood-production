package com.jinmifood.shop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinmifood.shop.config.properties.TossPaymentProperties;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;

@Component
public class TossPaymentClient implements PaymentGateway {
    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String secretKey;

    public TossPaymentClient(@Qualifier("tossRestClient") RestClient client,
            ObjectMapper objectMapper, TossPaymentProperties properties){
        this.client = client;
        this.objectMapper = objectMapper;
        this.secretKey = properties.getSecretKey();
    }

    @Override public PaymentResult confirm(String paymentKey,String orderId,long amount){
        requireConfigured();
        try{
            var body=client.post().uri("/v1/payments/confirm").headers(this::authorize)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("paymentKey",paymentKey,"orderId",orderId,"amount",amount))
                .retrieve().body(JsonNode.class);
            return result(body);
        }catch(RestClientResponseException e){throw paymentError(e);}
        catch(RestClientException e){throw unavailable();}
    }

    @Override public PaymentResult find(String paymentKey){
        requireConfigured();
        try{
            return result(client.get().uri("/v1/payments/{paymentKey}",paymentKey).headers(this::authorize)
                .retrieve().body(JsonNode.class));
        }catch(RestClientResponseException e){throw paymentError(e);}
        catch(RestClientException e){throw unavailable();}
    }

    @Override public PaymentResult cancel(String paymentKey,String reason,String idempotencyKey){
        requireConfigured();
        try{
            var body=client.post().uri("/v1/payments/{paymentKey}/cancel",paymentKey).headers(headers->{
                    authorize(headers);headers.set("Idempotency-Key",idempotencyKey);
                }).contentType(MediaType.APPLICATION_JSON).body(Map.of("cancelReason",reason))
                .retrieve().body(JsonNode.class);
            return result(body);
        }catch(RestClientResponseException e){throw paymentError(e);}
        catch(RestClientException e){throw unavailable();}
    }

    private void authorize(HttpHeaders headers){
        String token=Base64.getEncoder().encodeToString((secretKey+":").getBytes(StandardCharsets.UTF_8));
        headers.set(HttpHeaders.AUTHORIZATION,"Basic "+token);
    }
    private void requireConfigured(){if(secretKey==null||secretKey.isBlank())throw new PaymentException("PAYMENT_NOT_CONFIGURED","토스 시크릿키가 설정되지 않았습니다.");}
    private PaymentResult result(JsonNode node){
        if(node==null)throw new PaymentException("EMPTY_PAYMENT_RESPONSE","토스 결제 응답이 비어 있습니다.");
        String approved=node.path("approvedAt").asText(null);String receipt=node.path("receipt").path("url").asText(null);
        java.time.LocalDateTime approvedAt=null;
        if(approved!=null){try{approvedAt=OffsetDateTime.parse(approved).toLocalDateTime();}catch(java.time.format.DateTimeParseException e){throw new PaymentException("INVALID_PAYMENT_RESPONSE","토스 결제 승인시각 형식이 올바르지 않습니다.");}}
        return new PaymentResult(node.path("paymentKey").asText(null),node.path("orderId").asText(null),node.path("totalAmount").asLong(),
            node.path("status").asText(null),node.path("method").asText(null),approvedAt,receipt);
    }
    private PaymentException paymentError(RestClientResponseException error){
        try{
            JsonNode body=objectMapper.readTree(error.getResponseBodyAsString());
            String code=body.path("code").asText("TOSS_API_ERROR");
            String message=body.path("message").asText("결제 처리 중 오류가 발생했습니다.");
            return new PaymentException(code,message);
        }catch(Exception ignored){return new PaymentException("TOSS_API_ERROR","결제 처리 중 오류가 발생했습니다.");}
    }
    private PaymentException unavailable(){return new PaymentException("PAYMENT_GATEWAY_UNAVAILABLE","토스 결제 서버와 통신이 원활하지 않습니다. 잠시 후 다시 시도해 주세요.");}
}
