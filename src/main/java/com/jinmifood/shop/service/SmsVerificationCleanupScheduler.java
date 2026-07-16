package com.jinmifood.shop.service;

import com.jinmifood.shop.repository.SmsVerificationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Component
public class SmsVerificationCleanupScheduler {
    private final SmsVerificationRepository verifications;
    public SmsVerificationCleanupScheduler(SmsVerificationRepository verifications){this.verifications=verifications;}
    @Scheduled(cron="0 37 3 * * *",zone="Asia/Seoul")
    @Transactional public void deleteExpired(){verifications.deleteByExpiresAtBefore(LocalDateTime.now().minusDays(1));}
}
