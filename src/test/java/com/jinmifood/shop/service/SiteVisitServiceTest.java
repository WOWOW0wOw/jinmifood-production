package com.jinmifood.shop.service;

import com.jinmifood.shop.domain.SiteVisitDaily;
import com.jinmifood.shop.repository.SiteVisitDailyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static com.jinmifood.shop.JinmiShopApplication.BUSINESS_ZONE;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(SiteVisitService.class)
@ActiveProfiles("test")
class SiteVisitServiceTest {
    @Autowired SiteVisitService service;
    @Autowired SiteVisitDailyRepository visits;

    @Test
    void recordsAndReadsTodayAndTotalViews() {
        service.recordPageView();
        service.recordPageView();

        SiteVisitDaily previous = new SiteVisitDaily(LocalDate.now(BUSINESS_ZONE).minusDays(1));
        previous.increment();
        visits.save(previous);

        assertThat(service.todayViews()).isEqualTo(2);
        assertThat(service.totalViews()).isEqualTo(3);
    }

    @Test
    void returnsZeroBeforeAnyViewIsRecorded() {
        assertThat(service.todayViews()).isZero();
        assertThat(service.totalViews()).isZero();
    }
}
