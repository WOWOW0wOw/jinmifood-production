package com.jinmifood.shop.web;

import com.jinmifood.shop.domain.VerificationPurpose;
import com.jinmifood.shop.service.SmsVerificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/sms")
public class SmsVerificationController {
    private final SmsVerificationService verifications;
    public SmsVerificationController(SmsVerificationService verifications){this.verifications=verifications;}

    @PostMapping("/send") ResponseEntity<Map<String,Object>> send(@RequestParam String name,@RequestParam String birthDate,
        @RequestParam String phone,@RequestParam VerificationPurpose purpose){
        try{var identity=SmsIdentity.of(name,birthDate,phone);verifications.send(identity.phone(),purpose);return ok("인증번호를 발송했습니다. 5분 안에 입력해 주세요.");}
        catch(IllegalArgumentException|IllegalStateException e){return bad(e.getMessage());}
        catch(RuntimeException e){return bad("문자 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.");}
    }

    @PostMapping("/verify") ResponseEntity<Map<String,Object>> verify(@RequestParam String name,@RequestParam String birthDate,
        @RequestParam String phone,@RequestParam VerificationPurpose purpose,@RequestParam String code,HttpSession session){
        try{
            var identity=SmsIdentity.of(name,birthDate,phone);
            if(!verifications.verify(identity.phone(),purpose,code))return bad("인증번호가 올바르지 않습니다.");
            VerifiedPhoneProof.store(session,purpose,identity);
            return ok("이름·생년월일 입력과 휴대전화 인증이 완료되었습니다.");
        }catch(IllegalArgumentException|IllegalStateException e){return bad(e.getMessage());}
    }
    private ResponseEntity<Map<String,Object>> ok(String message){return ResponseEntity.ok(Map.of("success",true,"message",message));}
    private ResponseEntity<Map<String,Object>> bad(String message){return ResponseEntity.badRequest().body(Map.of("success",false,"message",message));}
}
