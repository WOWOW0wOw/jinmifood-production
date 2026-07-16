package com.jinmifood.shop.web;

import com.jinmifood.shop.service.SmsVerificationService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public record SmsIdentity(String name,LocalDate birthDate,String phone) {
    public static SmsIdentity of(String name,LocalDate birthDate,String phone){
        String normalizedName=normalizeName(name);
        if(birthDate==null)throw new IllegalArgumentException("생년월일을 입력해 주세요.");
        LocalDate today=LocalDate.now();
        if(birthDate.isAfter(today)||birthDate.isBefore(today.minusYears(120)))
            throw new IllegalArgumentException("생년월일을 확인해 주세요.");
        return new SmsIdentity(normalizedName,birthDate,SmsVerificationService.normalizePhone(phone));
    }
    public static SmsIdentity of(String name,String birthDate,String phone){
        try{return of(name,LocalDate.parse(birthDate),phone);}
        catch(DateTimeParseException|NullPointerException e){throw new IllegalArgumentException("생년월일을 확인해 주세요.");}
    }
    private static String normalizeName(String name){
        String value=name==null?"":name.trim().replaceAll("\\s+"," ");
        if(value.isBlank()||value.length()>40)throw new IllegalArgumentException("이름을 40자 이내로 입력해 주세요.");
        return value;
    }
}
