package com.jinmifood.shop.service;

import com.jinmifood.shop.domain.OrderStatus;
import com.jinmifood.shop.domain.PaymentStatus;
import com.jinmifood.shop.repository.CustomerOrderRepository;
import com.jinmifood.shop.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;

@Service
public class OrderRetentionService {
    private static final Duration MINIMUM_TTL = Duration.ofHours(1);
    private static final int MAX_BATCH_SIZE = 1_000;

    private final CustomerOrderRepository orders;
    private final ProductRepository products;

    public OrderRetentionService(CustomerOrderRepository orders, ProductRepository products) {
        this.orders = orders;
        this.products = products;
    }

    @Transactional
    public CleanupResult cleanupExpiredUnpaidOrders(Duration ttl, int requestedBatchSize) {
        if (ttl == null || ttl.compareTo(MINIMUM_TTL) < 0) {
            throw new IllegalArgumentException("미결제 주문 보관 기간은 1시간 이상이어야 합니다.");
        }
        int batchSize = Math.max(1, Math.min(requestedBatchSize, MAX_BATCH_SIZE));
        var cutoff = LocalDateTime.now().minus(ttl);
        var candidates = orders.findCleanupCandidates(
            OrderStatus.PAYMENT_PENDING,
            EnumSet.of(PaymentStatus.READY, PaymentStatus.FAILED),
            cutoff,
            PageRequest.of(0, batchSize)
        );

        int restoredStock = 0;
        int restoredPoints = 0;
        for (var order : candidates) {
            for (var item : order.getItems()) {
                var product = products.findForUpdate(item.getProductId()).orElse(null);
                if (product != null) {
                    product.increaseStock(item.getQuantity());
                    restoredStock += item.getQuantity();
                }
            }
            if (order.getMember() != null && order.getPointsUsed() > 0) {
                order.getMember().addPoints(order.getPointsUsed());
                restoredPoints += order.getPointsUsed();
            }
            orders.delete(order);
        }
        orders.flush();
        return new CleanupResult(candidates.size(), restoredStock, restoredPoints, cutoff);
    }

    public record CleanupResult(int deletedOrders, int restoredStock, int restoredPoints, LocalDateTime cutoff) {}
}
