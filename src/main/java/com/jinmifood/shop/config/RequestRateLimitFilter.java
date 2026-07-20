package com.jinmifood.shop.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestRateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_MILLIS = Duration.ofMinutes(15).toMillis();
    private static final int LOGIN_LIMIT = 10;
    private static final int REGISTER_LIMIT = 5;
    private static final int LOOKUP_LIMIT = 30;
    private static final int CHECKOUT_LIMIT = 30;
    private static final int WEBHOOK_LIMIT = 120;
    private static final int COMMUNITY_WRITE_LIMIT = 30;
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final ConcurrentHashMap<String, Window> requests = new ConcurrentHashMap<>();
    private final AtomicLong cleanupCounter = new AtomicLong();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        int limit = limitFor(request);
        if (limit == 0) {
            chain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        String key = request.getRequestURI() + ':' + clientAddress(request);
        if (cleanupCounter.incrementAndGet() % 500 == 0 || requests.size() >= MAX_TRACKED_KEYS) {
            requests.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= WINDOW_MILLIS);
        }
        if (requests.size() >= MAX_TRACKED_KEYS && !requests.containsKey(key)) {
            reject(response);
            return;
        }

        Window window = requests.compute(key, (ignored, current) ->
                current == null || now - current.startedAt >= WINDOW_MILLIS
                        ? new Window(now, 1) : new Window(current.startedAt, current.count + 1));
        if (window.count > limit) {
            reject(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setHeader("Retry-After", "900");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.sendError(429, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
    }

    private int limitFor(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return 0;
        if(request.getRequestURI().matches("^/products/\\d+/(reviews|inquiries)$"))return COMMUNITY_WRITE_LIMIT;
        return switch (request.getRequestURI()) {
            case "/login" -> LOGIN_LIMIT;
            case "/register" -> REGISTER_LIMIT;
            case "/api/sms/send" -> 5;
            case "/api/sms/verify", "/account/find-email", "/password/forgot", "/password/reset" -> 10;
            case "/order-lookup" -> LOOKUP_LIMIT;
            case "/checkout" -> CHECKOUT_LIMIT;
            case "/webhooks/toss" -> WEBHOOK_LIMIT;
            default -> 0;
        };
    }

    private String clientAddress(HttpServletRequest request) {
        String cloudflareAddress = request.getHeader("CF-Connecting-IP");
        if (isTrustedProxy(request.getRemoteAddr()) && isIpAddress(cloudflareAddress)) return cloudflareAddress.trim();
        return request.getRemoteAddr();
    }

    private boolean isTrustedProxy(String value) {
        if (!isIpAddress(value)) return false;
        try {
            InetAddress address = InetAddress.getByName(value.trim());
            return address.isLoopbackAddress() || address.isSiteLocalAddress();
        } catch (UnknownHostException ignored) {
            return false;
        }
    }

    private boolean isIpAddress(String value) {
        if (value == null) return false;
        String candidate = value.trim();
        if (candidate.isEmpty() || candidate.length() > 45 || !candidate.matches("[0-9A-Fa-f:.]+")) return false;
        try {
            InetAddress.getByName(candidate);
            return true;
        } catch (UnknownHostException ignored) {
            return false;
        }
    }

    private record Window(long startedAt, int count) {}
}
