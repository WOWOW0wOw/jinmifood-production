package com.jinmifood.shop.service;

import com.jinmifood.shop.domain.SmsVerification;
import com.jinmifood.shop.domain.VerificationPurpose;
import com.jinmifood.shop.repository.SmsVerificationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class SmsVerificationService {
    private static final Duration CODE_TTL=Duration.ofMinutes(5);
    private static final Duration RESEND_DELAY=Duration.ofSeconds(60);
    private final SmsVerificationRepository verifications;
    private final PasswordEncoder encoder;
    private final SmsSender sender;
    private final SecureRandom random=new SecureRandom();

    public SmsVerificationService(SmsVerificationRepository verifications,PasswordEncoder encoder,SmsSender sender){
        this.verifications=verifications;this.encoder=encoder;this.sender=sender;
    }

    @Transactional
    public void send(String rawPhone,VerificationPurpose purpose){
        String phone=normalizePhone(rawPhone);var now=LocalDateTime.now();
        var latest=verifications.findFirstByPhoneAndPurposeOrderByCreatedAtDesc(phone,purpose);
        if(latest.isPresent()&&latest.get().getCreatedAt().plus(RESEND_DELAY).isAfter(now))
            throw new IllegalStateException("인증번호는 60초 후 다시 요청할 수 있습니다.");
        String code=String.format("%06d",random.nextInt(1_000_000));
        sender.sendVerificationCode(phone,code);
        verifications.save(new SmsVerification(phone,purpose,encoder.encode(code),now.plus(CODE_TTL)));
    }

    @Transactional
    public boolean verify(String rawPhone,VerificationPurpose purpose,String code){
        String phone=normalizePhone(rawPhone);var now=LocalDateTime.now();
        var verification=verifications.findFirstByPhoneAndPurposeOrderByCreatedAtDesc(phone,purpose)
                .orElseThrow(()->new IllegalStateException("인증번호를 먼저 요청해 주세요."));
        if(!verification.canTry(now))throw new IllegalStateException("인증번호가 만료되었거나 입력 횟수를 초과했습니다.");
        if(code==null||!code.matches("^[0-9]{6}$")||!encoder.matches(code,verification.getCodeHash())){
            verification.recordFailure();
            return false;
        }
        verification.markVerified();
        return true;
    }

    public static String normalizePhone(String phone){
        String normalized=phone==null?"":phone.replaceAll("[^0-9]","");
        if(!normalized.matches("^01[016789][0-9]{7,8}$"))throw new IllegalArgumentException("휴대전화 번호 형식을 확인해 주세요.");
        return normalized;
    }
}
