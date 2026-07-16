package com.jinmifood.shop.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.web.multipart.support.MultipartFilter;

@Configuration
public class MultipartSecurityConfig {
    @Bean
    FilterRegistrationBean<MultipartFilter> multipartFilterRegistration(@Value("${app.multipart-filter.enabled:true}") boolean enabled){
        var registration=new FilterRegistrationBean<>(new MultipartFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setEnabled(enabled);
        return registration;
    }
}
