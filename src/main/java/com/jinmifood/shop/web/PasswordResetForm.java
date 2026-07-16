package com.jinmifood.shop.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PasswordResetForm {
    @NotBlank @Size(min=8,max=72,message="비밀번호는 8자 이상 72자 이하로 입력해 주세요")
    @Pattern(regexp="^(?=.*[A-Za-z])(?=.*\\d).+$",message="비밀번호에는 영문과 숫자가 모두 포함되어야 합니다") private String password;
    @NotBlank private String passwordConfirm;
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
    public String getPasswordConfirm(){return passwordConfirm;} public void setPasswordConfirm(String v){passwordConfirm=v;}
}
