package com.jinmifood.shop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "app.data-retention.enabled", havingValue = "true", matchIfMissing = true)
public class OrderRetentionScheduler {
    private static final Logger log = LoggerFactory.getLogger(OrderRetentionScheduler.class);

    private final OrderRetentionService retention;
    private final Duration unpaidOrderTtl;
    private final int batchSize;

    public OrderRetentionScheduler(OrderRetentionService retention,
            @Value("${app.data-retention.unpaid-order-ttl:7d}") Duration unpaidOrderTtl,
            @Value("${app.data-retention.batch-size:100}") int batchSize) {
        this.retention = retention;
        this.unpaidOrderTtl = unpaidOrderTtl;
        this.batchSize = batchSize;
    }

    @Scheduled(cron = "${app.data-retention.cleanup-cron:0 17 * * * *}",
        zone = "${app.data-retention.zone:Asia/Seoul}")
    public void cleanup() {
        try {
            var result = retention.cleanupExpiredUnpaidOrders(unpaidOrderTtl, batchSize);
            if (result.deletedOrders() > 0) {
                log.info("Expired unpaid order cleanup: deleted={}, restoredStock={}, restoredPoints={}",
                    result.deletedOrders(), result.restoredStock(), result.restoredPoints());
            }
        } catch (RuntimeException error) {
            log.error("Expired unpaid order cleanup failed", error);
        }
    }
}
