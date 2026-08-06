package com.jinmifood.shop.config;

import com.jinmifood.shop.service.SiteVisitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 20)
public class SiteVisitTrackingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(SiteVisitTrackingFilter.class);
    private static final Pattern ROBOT_USER_AGENT = Pattern.compile(
            "bot|crawler|spider|slurp|bingpreview|facebookexternalhit|uptimerobot|headlesschrome|curl|wget",
            Pattern.CASE_INSENSITIVE);
    private static final String[] EXCLUDED_PREFIXES = {
            "/admin", "/api", "/actuator", "/webhooks", "/css", "/js", "/images", "/uploads",
            "/fonts", "/favicon", "/robots.txt", "/sitemap.xml", "/error", "/.well-known"
    };

    private final SiteVisitService visits;

    public SiteVisitTrackingFilter(SiteVisitService visits) {
        this.visits = visits;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean candidate = isTrackableRequest(request);
        chain.doFilter(request, response);

        if (candidate && response.getStatus() >= 200 && response.getStatus() < 400
                && isHtml(response.getContentType())) {
            try {
                visits.recordPageView();
            } catch (RuntimeException exception) {
                log.warn("Failed to record page view for {}", request.getRequestURI(), exception);
            }
        }
    }

    private boolean isTrackableRequest(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) return false;
        String path = request.getRequestURI();
        for (String prefix : EXCLUDED_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) return false;
        }
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null && !userAgent.isBlank() && !ROBOT_USER_AGENT.matcher(userAgent).find();
    }

    private boolean isHtml(String contentType) {
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("text/html");
    }
}
