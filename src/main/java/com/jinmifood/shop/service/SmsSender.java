package com.jinmifood.shop.service;

public interface SmsSender {
    void sendVerificationCode(String phone,String code);
}
