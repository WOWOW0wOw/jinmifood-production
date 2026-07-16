package com.jinmifood.shop.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestRateLimitFilterTest {
    @Test
    void blocksTheSixthRegistrationFromTheSameAddress() throws Exception {
        var filter = new RequestRateLimitFilter();

        for (int i = 1; i <= 5; i++) {
            assertThat(invoke(filter, "POST", "/register", "192.0.2.10").getStatus()).isEqualTo(200);
        }

        var blocked = invoke(filter, "POST", "/register", "192.0.2.10");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("900");
        assertThat(blocked.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(blocked.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(blocked.getHeader("X-Frame-Options")).isEqualTo("DENY");
    }

    @Test
    void keepsDifferentAddressesAndSafeGetRequestsIndependent() throws Exception {
        var filter = new RequestRateLimitFilter();
        for (int i = 0; i < 10; i++) invoke(filter, "POST", "/login", "192.0.2.20");

        assertThat(invoke(filter, "POST", "/login", "192.0.2.20").getStatus()).isEqualTo(429);
        assertThat(invoke(filter, "POST", "/login", "192.0.2.21").getStatus()).isEqualTo(200);
        assertThat(invoke(filter, "GET", "/login", "192.0.2.20").getStatus()).isEqualTo(200);
    }

    @Test
    void usesCloudflareConnectingIpInsteadOfTheSharedTunnelAddress() throws Exception {
        var filter = new RequestRateLimitFilter();
        for (int i = 0; i < 10; i++) {
            invoke(filter, "POST", "/login", "172.20.0.4", "203.0.113.10");
        }

        assertThat(invoke(filter, "POST", "/login", "172.20.0.4", "203.0.113.10").getStatus()).isEqualTo(429);
        assertThat(invoke(filter, "POST", "/login", "172.20.0.4", "203.0.113.11").getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsInvalidForwardedAddressesAndFallsBackToTheConnectionAddress() throws Exception {
        var filter = new RequestRateLimitFilter();
        for (int i = 0; i < 10; i++) {
            invoke(filter, "POST", "/login", "192.0.2.30", "attacker.example");
        }

        assertThat(invoke(filter, "POST", "/login", "192.0.2.30", "198.51.100.4, 198.51.100.5").getStatus())
                .isEqualTo(429);
    }

    private MockHttpServletResponse invoke(RequestRateLimitFilter filter, String method, String uri, String address)
            throws Exception {
        return invoke(filter, method, uri, address, null);
    }

    private MockHttpServletResponse invoke(RequestRateLimitFilter filter, String method, String uri, String address,
            String cloudflareAddress) throws Exception {
        var request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(address);
        if (cloudflareAddress != null) request.addHeader("CF-Connecting-IP", cloudflareAddress);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
