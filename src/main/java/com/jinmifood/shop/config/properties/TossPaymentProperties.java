package com.jinmifood.shop.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.payment.toss")
public class TossPaymentProperties {
    public enum Mode { TEST, LIVE }

    @NotNull
    private URI apiBaseUrl;
    @NotNull
    private Mode mode = Mode.TEST;
    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(3);
    @NotNull
    private Duration readTimeout = Duration.ofSeconds(10);
    private String clientKey = "";
    private String secretKey = "";

    public URI getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(URI apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
    public String getClientKey() { return clientKey; }
    public void setClientKey(String clientKey) { this.clientKey = clientKey == null ? "" : clientKey.strip(); }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey == null ? "" : secretKey.strip(); }

    public boolean isConfigured() {
        return !clientKey.isBlank() && !secretKey.isBlank();
    }

    public String modeLabel() {
        return mode.name().toLowerCase();
    }

    @AssertTrue(message = "TOSS_CLIENT_KEY and TOSS_SECRET_KEY must be provided together")
    public boolean isKeyPairComplete() {
        return clientKey.isBlank() == secretKey.isBlank();
    }

    @AssertTrue(message = "Toss key prefixes must match TOSS_MODE (test_ck/test_sk or live_ck/live_sk)")
    public boolean isKeyModeConsistent() {
        if (!isConfigured()) return true;
        String prefix = mode == Mode.LIVE ? "live_" : "test_";
        return clientKey.startsWith(prefix + "ck_") && secretKey.startsWith(prefix + "sk_");
    }

    @AssertTrue(message = "Toss timeouts must be greater than zero")
    public boolean areTimeoutsValid() {
        return connectTimeout != null && readTimeout != null
                && !connectTimeout.isZero() && !connectTimeout.isNegative()
                && !readTimeout.isZero() && !readTimeout.isNegative();
    }

    @Override
    public String toString() {
        return "TossPaymentProperties{mode=" + mode + ", configured=" + isConfigured()
                + ", apiBaseUrl=" + apiBaseUrl + '}';
    }
}
