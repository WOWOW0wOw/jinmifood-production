package com.jinmifood.shop.config;

import com.jinmifood.shop.config.properties.TossPaymentProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class PaymentClientConfig {
    @Bean("tossRestClient")
    RestClient tossRestClient(RestClient.Builder builder, TossPaymentProperties properties) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return builder.baseUrl(properties.getApiBaseUrl().toString())
                .requestFactory(requestFactory)
                .build();
    }
}
