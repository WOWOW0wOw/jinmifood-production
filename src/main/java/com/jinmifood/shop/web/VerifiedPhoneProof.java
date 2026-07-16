package com.jinmifood.shop.web;

import com.jinmifood.shop.domain.VerificationPurpose;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalDate;

public record VerifiedPhoneProof(VerificationPurpose purpose,String name,LocalDate birthDate,String phone,LocalDateTime expiresAt) implements Serializable {
    private static final String PREFIX="verified-phone:";
    public static void store(HttpSession session,VerificationPurpose purpose,SmsIdentity identity){
        session.setAttribute(PREFIX+purpose,new VerifiedPhoneProof(purpose,identity.name(),identity.birthDate(),identity.phone(),LocalDateTime.now().plusMinutes(10)));
    }
    public static boolean matches(HttpSession session,VerificationPurpose purpose,SmsIdentity identity){
        Object value=session.getAttribute(PREFIX+purpose);
        return value instanceof VerifiedPhoneProof proof&&proof.purpose==purpose&&proof.name.equals(identity.name())
            &&proof.birthDate.equals(identity.birthDate())&&proof.phone.equals(identity.phone())&&proof.expiresAt.isAfter(LocalDateTime.now());
    }
    public static void consume(HttpSession session,VerificationPurpose purpose){session.removeAttribute(PREFIX+purpose);}
}
