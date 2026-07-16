package com.jinmifood.shop.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("app.social")
public class SocialLoginProperties {
    private Provider google=new Provider();
    private Provider kakao=new Provider();
    private Provider naver=new Provider();

    public List<String> configuredProviders(){
        var result=new ArrayList<String>();
        if(google.isConfigured())result.add("google");
        if(kakao.isConfigured())result.add("kakao");
        if(naver.isConfigured())result.add("naver");
        return List.copyOf(result);
    }
    public Provider provider(String id){return switch(id){case "google"->google;case "kakao"->kakao;case "naver"->naver;default->throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.");};}
    public Provider getGoogle(){return google;} public void setGoogle(Provider v){google=v;}
    public Provider getKakao(){return kakao;} public void setKakao(Provider v){kakao=v;}
    public Provider getNaver(){return naver;} public void setNaver(Provider v){naver=v;}

    public static class Provider {
        private String clientId="";
        private String clientSecret="";
        public boolean isConfigured(){return clientId!=null&&!clientId.isBlank()&&clientSecret!=null&&!clientSecret.isBlank();}
        public String getClientId(){return clientId;} public void setClientId(String v){clientId=v;}
        public String getClientSecret(){return clientSecret;} public void setClientSecret(String v){clientSecret=v;}
    }
}
