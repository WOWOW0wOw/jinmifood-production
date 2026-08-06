package com.jinmifood.shop.service;

import com.jinmifood.shop.domain.SiteVisitDaily;
import com.jinmifood.shop.repository.SiteVisitDailyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

import static com.jinmifood.shop.JinmiShopApplication.BUSINESS_ZONE;

@Service
public class SiteVisitService {
    private final SiteVisitDailyRepository visits;
    private final TransactionTemplate transaction;

    public SiteVisitService(SiteVisitDailyRepository visits, PlatformTransactionManager transactionManager) {
        this.visits = visits;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public synchronized void recordPageView() {
        transaction.executeWithoutResult(status -> {
            LocalDate today = LocalDate.now(BUSINESS_ZONE);
            SiteVisitDaily daily = visits.findById(today).orElseGet(() -> new SiteVisitDaily(today));
            daily.increment();
            visits.save(daily);
        });
    }

    @Transactional(readOnly = true)
    public long todayViews() {
        return visits.findById(LocalDate.now(BUSINESS_ZONE))
                .map(SiteVisitDaily::getViewCount)
                .orElse(0L);
    }

    @Transactional(readOnly = true)
    public long totalViews() {
        return visits.sumViewCount();
    }
}
