package com.jinmifood.shop.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.sms")
public class SmsProperties {
    public enum Mode { DISABLED, SOLAPI }
    private Mode mode = Mode.DISABLED;
    private String apiKey = "";
    private String apiSecret = "";
    private String sender = "";
    private boolean requireVerification = true;

    public boolean isConfigured(){return mode==Mode.SOLAPI&&hasText(apiKey)&&hasText(apiSecret)&&hasText(sender);}
    private boolean hasText(String value){return value!=null&&!value.isBlank();}
    public Mode getMode(){return mode;} public void setMode(Mode v){mode=v;}
    public String getApiKey(){return apiKey;} public void setApiKey(String v){apiKey=v;}
    public String getApiSecret(){return apiSecret;} public void setApiSecret(String v){apiSecret=v;}
    public String getSender(){return sender;} public void setSender(String v){sender=v;}
    public boolean isRequireVerification(){return requireVerification;} public void setRequireVerification(boolean v){requireVerification=v;}
}
