package com.jinmifood.shop.web;
import jakarta.validation.constraints.*;
public class CheckoutForm {
    @NotBlank @Size(max=40) private String customerName;
    @NotBlank @Pattern(regexp="^[0-9-]{9,20}$",message="전화번호 형식을 확인해 주세요") private String phone;
    @NotBlank @Email @Size(max=120) private String email;
    @NotBlank @Size(max=10) private String postalCode;
    @NotBlank @Size(max=200) private String address;
    @Size(max=200) private String addressDetail;
    @Size(max=200) private String deliveryMemo;
    @PositiveOrZero private int pointsToUse;
    @AssertTrue(message="구매 조건 및 개인정보 수집에 동의해 주세요") private boolean agreed;
    private boolean rememberAddress=true;
    public String getCustomerName(){return customerName;} public void setCustomerName(String v){customerName=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;} public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPostalCode(){return postalCode;} public void setPostalCode(String v){postalCode=v;} public String getAddress(){return address;} public void setAddress(String v){address=v;}
    public String getAddressDetail(){return addressDetail;} public void setAddressDetail(String v){addressDetail=v;} public String getDeliveryMemo(){return deliveryMemo;} public void setDeliveryMemo(String v){deliveryMemo=v;}
    public boolean isAgreed(){return agreed;} public void setAgreed(boolean v){agreed=v;}
    public int getPointsToUse(){return pointsToUse;} public void setPointsToUse(int v){pointsToUse=v;}
    public boolean isRememberAddress(){return rememberAddress;} public void setRememberAddress(boolean v){rememberAddress=v;}
}
