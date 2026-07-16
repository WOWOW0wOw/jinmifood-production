package com.jinmifood.shop.repository;

import com.jinmifood.shop.domain.SmsVerification;
import com.jinmifood.shop.domain.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface SmsVerificationRepository extends JpaRepository<SmsVerification,Long> {
    Optional<SmsVerification> findFirstByPhoneAndPurposeOrderByCreatedAtDesc(String phone,VerificationPurpose purpose);
    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}
