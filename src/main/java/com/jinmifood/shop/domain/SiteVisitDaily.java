package com.jinmifood.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "site_visit_daily")
public class SiteVisitDaily {
    @Id
    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    protected SiteVisitDaily() {}

    public SiteVisitDaily(LocalDate visitDate) {
        this.visitDate = visitDate;
    }

    public void increment() {
        viewCount++;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public long getViewCount() {
        return viewCount;
    }
}
