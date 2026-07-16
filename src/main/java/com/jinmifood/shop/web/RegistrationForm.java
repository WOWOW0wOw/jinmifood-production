package com.jinmifood.shop.web;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public class RegistrationForm {
    @NotBlank @Email @Size(max=120) private String email;
    @NotBlank @Size(min=8,max=72,message="비밀번호는 8자 이상 72자 이하로 입력해 주세요")
    @Pattern(regexp="^(?=.*[A-Za-z])(?=.*\\d).+$",message="비밀번호에는 영문과 숫자가 모두 포함되어야 합니다") private String password;
    @NotBlank private String passwordConfirm;
    @NotBlank @Size(max=40) private String name;
    @NotNull @PastOrPresent @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate birthDate;
    @NotBlank @Pattern(regexp="^[0-9-]{9,20}$",message="전화번호 형식을 확인해 주세요") private String phone;
    public String getEmail(){return email;} public void setEmail(String v){email=v==null?null:v.trim().toLowerCase();}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
    public String getPasswordConfirm(){return passwordConfirm;} public void setPasswordConfirm(String v){passwordConfirm=v;}
    public String getName(){return name;} public void setName(String v){name=v;} public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public LocalDate getBirthDate(){return birthDate;} public void setBirthDate(LocalDate v){birthDate=v;}
}
