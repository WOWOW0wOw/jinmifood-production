package com.jinmifood.shop.config;

import com.jinmifood.shop.config.properties.AdminProperties;
import com.jinmifood.shop.config.properties.AppProperties;
import com.jinmifood.shop.config.properties.SmsProperties;
import com.jinmifood.shop.config.properties.SocialLoginProperties;
import com.jinmifood.shop.config.properties.TossPaymentProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class ProductionConfigurationValidator implements InitializingBean {
    private final Environment environment;
    private final AppProperties app;
    private final AdminProperties admin;
    private final TossPaymentProperties toss;
    private final SmsProperties sms;
    private final SocialLoginProperties social;

    public ProductionConfigurationValidator(Environment environment, AppProperties app,
            AdminProperties admin, TossPaymentProperties toss, SmsProperties sms,
            SocialLoginProperties social) {
        this.environment = environment;
        this.app = app;
        this.admin = admin;
        this.toss = toss;
        this.sms = sms;
        this.social = social;
    }

    @Override
    public void afterPropertiesSet() {
        if (Arrays.stream(environment.getActiveProfiles()).noneMatch("prod"::equals)) return;
        if (!"https".equalsIgnoreCase(app.getPublicBaseUrl().getScheme())) {
            throw new IllegalStateException("Production APP_PUBLIC_BASE_URL must use HTTPS");
        }
        if (!toss.isConfigured()) {
            throw new IllegalStateException("Production requires both TOSS_CLIENT_KEY and TOSS_SECRET_KEY");
        }
        String keys = (toss.getClientKey() + toss.getSecretKey()).toLowerCase();
        if (keys.contains("replace") || keys.contains("example") || keys.contains("placeholder")) {
            throw new IllegalStateException("Production Toss keys must not use example values");
        }
        String password = admin.getPassword();
        if (password.length() < 12 || "Admin!12345".equals(password)
                || password.toLowerCase().contains("change-this")) {
            throw new IllegalStateException("Production ADMIN_PASSWORD must contain at least 12 characters and must not use an example value");
        }
        validateSms();
        validateProvider("Google", social.getGoogle());
        validateProvider("Kakao", social.getKakao());
        validateProvider("Naver", social.getNaver());
    }

    private void validateSms() {
        boolean anyCredential = hasText(sms.getApiKey()) || hasText(sms.getApiSecret()) || hasText(sms.getSender());
        if (sms.getMode() == SmsProperties.Mode.SOLAPI && !sms.isConfigured()) {
            throw new IllegalStateException("SMS_MODE=solapi requires SOLAPI_API_KEY, SOLAPI_API_SECRET and SOLAPI_SENDER");
        }
        if (sms.getMode() == SmsProperties.Mode.DISABLED && anyCredential) {
            throw new IllegalStateException("SOLAPI credentials are present but SMS_MODE is disabled");
        }
        if (sms.isConfigured()) {
            rejectExample("SOLAPI credentials", sms.getApiKey(), sms.getApiSecret(), sms.getSender());
            if (!sms.getSender().matches("01[016789]\\d{7,8}")) {
                throw new IllegalStateException("SOLAPI_SENDER must be a Korean mobile number containing digits only");
            }
        }
    }

    private void validateProvider(String name, SocialLoginProperties.Provider provider) {
        boolean hasId = hasText(provider.getClientId());
        boolean hasSecret = hasText(provider.getClientSecret());
        if (hasId != hasSecret) {
            throw new IllegalStateException(name + " social login requires both client ID and client secret");
        }
        if (hasId) rejectExample(name + " social login credentials", provider.getClientId(), provider.getClientSecret());
    }

    private void rejectExample(String label, String... values) {
        String joined = String.join("", values).toLowerCase();
        if (joined.contains("replace") || joined.contains("example") || joined.contains("placeholder")) {
            throw new IllegalStateException(label + " must not use example values");
        }
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
}
