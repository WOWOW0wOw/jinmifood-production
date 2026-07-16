package com.jinmifood.shop.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.jinmifood.shop.config.properties.AppProperties;
import com.jinmifood.shop.config.properties.TossPaymentProperties;
import com.jinmifood.shop.domain.PaymentStatus;
import com.jinmifood.shop.repository.CustomerOrderRepository;
import com.jinmifood.shop.service.PaymentException;
import com.jinmifood.shop.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Pattern;

@Controller
public class PaymentController {
    private static final Pattern ORDER_NUMBER = Pattern.compile("JM[0-9]{14}[0-9A-F]{6}");
    private static final int MAX_PAYMENT_KEY_LENGTH = 200;
    private static final int MAX_ERROR_CODE_LENGTH = 80;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 200;

    private final PaymentService payments;
    private final CustomerOrderRepository orders;
    private final String clientKey;
    private final String publicBaseUrl;

    public PaymentController(PaymentService payments, CustomerOrderRepository orders,
            TossPaymentProperties toss, AppProperties app) {
        this.payments = payments;
        this.orders = orders;
        this.clientKey = toss.getClientKey();
        this.publicBaseUrl = app.publicBaseUrl();
    }

    @GetMapping("/payments/toss/request/{orderNumber}")
    String request(@PathVariable String orderNumber, HttpSession session, Model model) {
        try {
            requireValidOrderNumber(orderNumber);
            requireSessionOrder(session, orderNumber);
            var order = payments.prepare(orderNumber);
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                model.addAttribute("order", order);
                return "store/order-complete";
            }
            model.addAttribute("order", order);
            model.addAttribute("clientKey", clientKey);
            model.addAttribute("paymentConfigured", clientKey != null && !clientKey.isBlank());
            model.addAttribute("successUrl", publicBaseUrl + "/payments/toss/success");
            model.addAttribute("failUrl", publicBaseUrl + "/payments/toss/fail");
            String first = order.getItems().isEmpty() ? "진미푸드 상품" : order.getItems().getFirst().getProductName();
            model.addAttribute("orderName", order.getItems().size() > 1
                    ? first + " 외 " + (order.getItems().size() - 1) + "건" : first);
            return "store/payment";
        } catch (PaymentException e) {
            return failure(model, orderNumber, e.getCode(), e.getMessage(), owns(session, orderNumber));
        } catch (NoSuchElementException e) {
            return failure(model, orderNumber, "ORDER_NOT_FOUND", e.getMessage(), false);
        }
    }

    @GetMapping("/payments/toss/success")
    String success(@RequestParam String paymentKey, @RequestParam String orderId,
            @RequestParam long amount, HttpSession session, Model model) {
        try {
            requirePaymentParameters(paymentKey, orderId, amount);
            requireSessionOrder(session, orderId);
            var order = payments.confirm(orderId, paymentKey, amount);
            session.removeAttribute("pendingPaymentOrderNumber");
            model.addAttribute("order", order);
            model.addAttribute("cartCount", 0);
            return "store/order-complete";
        } catch (PaymentException e) {
            if (owns(session, orderId)) payments.recordFailure(orderId, e.getCode(), e.getMessage());
            return failure(model, orderId, e.getCode(), e.getMessage(), owns(session, orderId));
        } catch (NoSuchElementException e) {
            return failure(model, orderId, "ORDER_NOT_FOUND", e.getMessage(), false);
        }
    }

    @GetMapping("/payments/toss/fail")
    String fail(@RequestParam String code, @RequestParam String message,
            @RequestParam(required = false) String orderId, HttpSession session, Model model) {
        String safeCode = bounded(code, MAX_ERROR_CODE_LENGTH, "PAYMENT_FAILED");
        String safeMessage = bounded(message, MAX_ERROR_MESSAGE_LENGTH, "결제를 완료하지 못했습니다.");
        String pending = (String) session.getAttribute("pendingPaymentOrderNumber");
        String target = validOrderNumber(orderId) ? orderId : pending;
        if (target != null && Objects.equals(pending, target)) payments.recordFailure(target, safeCode, safeMessage);
        return failure(model, target, safeCode, safeMessage, Objects.equals(pending, target));
    }

    @PostMapping(value = "/webhooks/toss", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Void> webhook(@RequestBody JsonNode event) {
        try {
            if ("PAYMENT_STATUS_CHANGED".equals(event.path("eventType").asText())) {
                JsonNode data = event.path("data");
                String paymentKey = data.path("paymentKey").asText();
                String orderId = data.path("orderId").asText();
                requirePaymentParameters(paymentKey, orderId, 0);
                payments.syncWebhook(paymentKey, orderId);
            }
            return ResponseEntity.ok().build();
        } catch (PaymentException e) {
            return ResponseEntity.status("PAYMENT_GATEWAY_UNAVAILABLE".equals(e.getCode())
                    ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_REQUEST).build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private String failure(Model model, String orderNumber, String code, String message, boolean allowRetry) {
        model.addAttribute("orderNumber", orderNumber);
        model.addAttribute("code", code);
        model.addAttribute("message", message);
        if (allowRetry && orderNumber != null) {
            orders.findByOrderNumber(orderNumber).ifPresent(order -> model.addAttribute("order", order));
        }
        return "store/payment-fail";
    }

    private void requireSessionOrder(HttpSession session, String orderNumber) {
        if (!Objects.equals(session.getAttribute("pendingPaymentOrderNumber"), orderNumber)) {
            throw new PaymentException("INVALID_PAYMENT_SESSION", "결제를 시작한 브라우저에서 다시 시도해 주세요.");
        }
    }

    private void requirePaymentParameters(String paymentKey, String orderId, long amount) {
        requireValidOrderNumber(orderId);
        if (paymentKey == null || paymentKey.isBlank() || paymentKey.length() > MAX_PAYMENT_KEY_LENGTH || amount < 0) {
            throw new PaymentException("INVALID_PAYMENT_REQUEST", "유효하지 않은 결제 요청입니다.");
        }
    }

    private void requireValidOrderNumber(String orderNumber) {
        if (!validOrderNumber(orderNumber)) {
            throw new PaymentException("INVALID_ORDER_NUMBER", "유효하지 않은 주문번호입니다.");
        }
    }

    private boolean validOrderNumber(String orderNumber) {
        return orderNumber != null && ORDER_NUMBER.matcher(orderNumber).matches();
    }

    private boolean owns(HttpSession session, String orderNumber) {
        return Objects.equals(session.getAttribute("pendingPaymentOrderNumber"), orderNumber);
    }

    private static String bounded(String value, int maxLength, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

}
