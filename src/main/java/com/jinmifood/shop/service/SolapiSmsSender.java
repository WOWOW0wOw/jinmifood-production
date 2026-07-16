package com.jinmifood.shop.service;

import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.model.Message;
import com.jinmifood.shop.config.properties.SmsProperties;
import org.springframework.stereotype.Component;

@Component
public class SolapiSmsSender implements SmsSender {
    private final SmsProperties properties;
    public SolapiSmsSender(SmsProperties properties){this.properties=properties;}

    @Override public void sendVerificationCode(String phone,String code){
        if(!properties.isConfigured())throw new IllegalStateException("문자인증 서비스가 아직 설정되지 않았습니다.");
        var service=SolapiClient.INSTANCE.createInstance(properties.getApiKey(),properties.getApiSecret());
        var message=new Message();
        message.setFrom(normalize(properties.getSender()));
        message.setTo(normalize(phone));
        message.setText("[진미푸드] 인증번호는 "+code+"입니다. 5분 안에 입력해 주세요.");
        try{service.send(message,null);}catch(Exception e){throw new IllegalStateException("문자 발송 서비스 요청에 실패했습니다.",e);}
    }
    private String normalize(String phone){return phone==null?"":phone.replaceAll("[^0-9]","");}
}
