package com.jinmifood.shop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.nio.file.Paths;
import java.time.Duration;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String uploadDir;
    public WebConfig(@Value("${app.upload.dir:./uploads}") String uploadDir){this.uploadDir=uploadDir;}
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry){
        String location=Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if(!location.endsWith("/"))location+="/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(location)
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
        registry.addResourceHandler("/images/**").addResourceLocations("classpath:/static/images/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic().mustRevalidate());
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic().mustRevalidate());
    }
}
