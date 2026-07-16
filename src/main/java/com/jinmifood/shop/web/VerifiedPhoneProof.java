package com.jinmifood.shop.web;

import com.jinmifood.shop.domain.VerificationPurpose;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.time.LocalDateTime;

public record VerifiedPhoneProof(VerificationPurpose purpose,String phone,LocalDateTime expiresAt) implements Serializable {
    private static final String PREFIX="verified-phone:";
    public static void store(HttpSession session,VerificationPurpose purpose,String phone){session.setAttribute(PREFIX+purpose,new VerifiedPhoneProof(purpose,phone,LocalDateTime.now().plusMinutes(10)));}
    public static boolean matches(HttpSession session,VerificationPurpose purpose,String phone){
        Object value=session.getAttribute(PREFIX+purpose);
        return value instanceof VerifiedPhoneProof proof&&proof.purpose==purpose&&proof.phone.equals(phone)&&proof.expiresAt.isAfter(LocalDateTime.now());
    }
    public static void consume(HttpSession session,VerificationPurpose purpose){session.removeAttribute(PREFIX+purpose);}
}
