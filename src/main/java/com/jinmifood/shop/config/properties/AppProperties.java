package com.jinmifood.shop.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    @NotNull
    private URI publicBaseUrl;

    public URI getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(URI publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String publicBaseUrl() {
        String value = publicBaseUrl.toString();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    @AssertTrue(message = "app.public-base-url must be an absolute HTTP(S) URL without credentials, query, or fragment")
    public boolean isPublicBaseUrlValid() {
        if (publicBaseUrl == null) return true;
        String scheme = publicBaseUrl.getScheme();
        return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                && publicBaseUrl.getHost() != null
                && publicBaseUrl.getUserInfo() == null
                && publicBaseUrl.getQuery() == null
                && publicBaseUrl.getFragment() == null;
    }
}
