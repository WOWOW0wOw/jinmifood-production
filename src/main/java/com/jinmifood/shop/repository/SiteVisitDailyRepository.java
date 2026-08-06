package com.jinmifood.shop.repository;

import com.jinmifood.shop.domain.SiteVisitDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;

public interface SiteVisitDailyRepository extends JpaRepository<SiteVisitDaily, LocalDate> {
    @Query("select coalesce(sum(v.viewCount), 0) from SiteVisitDaily v")
    long sumViewCount();
}
