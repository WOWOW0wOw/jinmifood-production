package com.jinmifood.shop.config;

import com.jinmifood.shop.service.SiteVisitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.mockito.Mockito.*;

class SiteVisitTrackingFilterTest {
    private SiteVisitService visits;
    private SiteVisitTrackingFilter filter;

    @BeforeEach
    void setUp() {
        visits = mock(SiteVisitService.class);
        filter = new SiteVisitTrackingFilter(visits);
    }

    @Test
    void recordsSuccessfulPublicHtmlPage() throws Exception {
        execute("GET", "/products", "Mozilla/5.0", 200, "text/html;charset=UTF-8");
        verify(visits).recordPageView();
    }

    @Test
    void excludesAdminAndStaticResources() throws Exception {
        execute("GET", "/admin", "Mozilla/5.0", 200, "text/html");
        execute("GET", "/images/product.jpg", "Mozilla/5.0", 200, "image/jpeg");
        verifyNoInteractions(visits);
    }

    @Test
    void excludesRobotsAndCommandLineClients() throws Exception {
        execute("GET", "/", "Googlebot/2.1", 200, "text/html");
        execute("GET", "/products", "curl/8.0", 200, "text/html");
        verifyNoInteractions(visits);
    }

    @Test
    void excludesPostRequests() throws Exception {
        execute("POST", "/login", "Mozilla/5.0", 200, "text/html");
        verifyNoInteractions(visits);
    }

    @Test
    void excludesErrorsAndNonHtmlResponses() throws Exception {
        execute("GET", "/missing", "Mozilla/5.0", 404, "text/html");
        execute("GET", "/data", "Mozilla/5.0", 200, "application/json");
        verifyNoInteractions(visits);
    }

    @Test
    void trackingFailureDoesNotBreakThePageResponse() throws Exception {
        doThrow(new IllegalStateException("database unavailable")).when(visits).recordPageView();
        execute("GET", "/", "Mozilla/5.0", 200, "text/html");
        verify(visits).recordPageView();
    }

    private void execute(String method, String path, String userAgent, int status, String contentType)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.addHeader("User-Agent", userAgent);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            response.setStatus(status);
            response.setContentType(contentType);
        });
    }
}
