package com.jinmifood.shop.config;

import com.jinmifood.shop.config.properties.AdminProperties;
import com.jinmifood.shop.config.properties.AppProperties;
import com.jinmifood.shop.config.properties.SocialLoginProperties;
import com.jinmifood.shop.config.properties.SmsProperties;
import com.jinmifood.shop.config.properties.TossPaymentProperties;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigurationMaintenanceTest {
    @Test
    void completeSourceCatalogHasMainAndDetailAssets() throws Exception {
        var sourceIds = List.of(
                "1746168600", "1746168499", "1746168229", "1746168000", "1746168960",
                "1746169070", "1746168838", "1746167513", "1746168728", "1746167889",
                "1746167773", "1746166839", "1746168372", "1746167375", "1746166486",
                "1746166104", "1746169275", "1746169174", "1746168110", "1746167666",
                "1746167191", "1746166995");

        assertThat(sourceIds).hasSize(22).doesNotHaveDuplicates();
        for (var sourceId : sourceIds) {
            try (var main = getClass().getResourceAsStream("/static/images/catalog/" + sourceId + "-main.jpg");
                 var detail = getClass().getResourceAsStream("/static/images/catalog/" + sourceId + "-detail.jpg")) {
                assertThat(main).as("main image for %s", sourceId).isNotNull();
                assertThat(detail).as("detail image for %s", sourceId).isNotNull();
                assertThat(main.readNBytes(2)).containsExactly((byte) 0xff, (byte) 0xd8);
                assertThat(detail.readNBytes(2)).containsExactly((byte) 0xff, (byte) 0xd8);
            }
        }
    }

    @Test
    void mobileHeaderKeepsLoginAndCartActionsVisible() throws Exception {
        try (var input = getClass().getResourceAsStream("/static/css/style.css")) {
            assertThat(input).isNotNull();
            var css = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(css)
                    .doesNotContain(".quick a:first-child{display:none}")
                    .contains(".quick{margin-left:auto;align-items:center;white-space:nowrap}")
                    .contains(".quick{order:2;justify-content:flex-end}");
        }
    }

    @Test
    void requiresTossClientAndSecretKeysTogether() {
        var toss = toss(TossPaymentProperties.Mode.TEST, "test_ck_valid", "");
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(toss))
                    .extracting(violation -> violation.getMessage())
                    .contains("TOSS_CLIENT_KEY and TOSS_SECRET_KEY must be provided together");
        }
    }

    @Test
    void rejectsKeysThatDoNotMatchConfiguredMode() {
        var toss = toss(TossPaymentProperties.Mode.LIVE, "test_ck_valid", "test_sk_valid");
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(toss))
                    .extracting(violation -> violation.getMessage())
                    .contains("Toss key prefixes must match TOSS_MODE (test_ck/test_sk or live_ck/live_sk)");
        }
    }

    @Test
    void neverIncludesSecretsInConfigurationText() {
        var toss = toss(TossPaymentProperties.Mode.TEST, "test_ck_sensitive", "test_sk_sensitive");
        assertThat(toss.toString()).doesNotContain("test_ck_sensitive", "test_sk_sensitive")
                .contains("configured=true", "mode=TEST");
    }

    @Test
    void productionRejectsExampleSecretsBeforeServingTraffic() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        var app = new AppProperties();
        app.setPublicBaseUrl(URI.create("https://shop.example.com"));
        var admin = new AdminProperties();
        admin.setUsername("admin");
        admin.setPassword("StrongPassword!123");
        var toss = toss(TossPaymentProperties.Mode.LIVE,
                "live_ck_replace_with_real_key", "live_sk_replace_with_real_key");

        var validator = new ProductionConfigurationValidator(environment, app, admin, toss,
                new SmsProperties(), new SocialLoginProperties());
        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("example values");
    }

    @Test
    void productionRejectsHalfConfiguredSocialProvider() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        var app = new AppProperties();
        app.setPublicBaseUrl(URI.create("https://jinmifood.com"));
        var admin = new AdminProperties();
        admin.setUsername("admin");
        admin.setPassword("StrongPassword!123");
        var social = new SocialLoginProperties();
        social.getKakao().setClientId("configured-client-id");

        var validator = new ProductionConfigurationValidator(environment, app, admin,
                toss(TossPaymentProperties.Mode.TEST, "test_ck_valid", "test_sk_valid"),
                new SmsProperties(), social);

        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Kakao social login requires both");
    }

    @Test
    void kakaoRequestsOnlyTheApprovedNicknameScope() {
        var social = new SocialLoginProperties();
        social.getKakao().setClientId("test-kakao-client");
        social.getKakao().setClientSecret("test-kakao-secret");

        var registrations = new SocialLoginConfig().clientRegistrationRepository(social);
        assertThat(registrations.findByRegistrationId("kakao").getScopes())
                .containsExactly("profile_nickname");
    }

    private TossPaymentProperties toss(TossPaymentProperties.Mode mode, String clientKey, String secretKey) {
        var toss = new TossPaymentProperties();
        toss.setMode(mode);
        toss.setApiBaseUrl(URI.create("https://api.tosspayments.com"));
        toss.setClientKey(clientKey);
        toss.setSecretKey(secretKey);
        return toss;
    }
}
