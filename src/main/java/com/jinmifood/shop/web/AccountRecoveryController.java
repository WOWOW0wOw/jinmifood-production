package com.jinmifood.shop.web;

import com.jinmifood.shop.config.properties.SmsProperties;
import com.jinmifood.shop.domain.VerificationPurpose;
import com.jinmifood.shop.repository.MemberRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;

@Controller
public class AccountRecoveryController {
    private static final String RESET_MEMBER="password-reset-member";
    private static final String RESET_EXPIRES="password-reset-expires";
    private final MemberRepository members;private final PasswordEncoder encoder;private final SmsProperties sms;
    public AccountRecoveryController(MemberRepository members,PasswordEncoder encoder,SmsProperties sms){this.members=members;this.encoder=encoder;this.sms=sms;}

    @GetMapping("/account/find-email") String findEmail(Model model){model.addAttribute("smsConfigured",sms.isConfigured());return "find-email";}
    @PostMapping("/account/find-email") String findEmailResult(@RequestParam String name,@RequestParam String birthDate,
        @RequestParam String phone,HttpSession session,Model model){
        model.addAttribute("smsConfigured",sms.isConfigured());SmsIdentity identity=identity(name,birthDate,phone,model);if(identity==null)return "find-email";
        if(!VerifiedPhoneProof.matches(session,VerificationPurpose.FIND_EMAIL,identity)){model.addAttribute("error","이름·생년월일 입력 후 문자인증을 먼저 완료해 주세요.");return "find-email";}
        var emails=members.findByNameAndBirthDateAndPhoneOrderByCreatedAtAsc(identity.name(),identity.birthDate(),identity.phone()).stream().map(m->mask(m.getEmail())).toList();
        model.addAttribute("emails",emails);VerifiedPhoneProof.consume(session,VerificationPurpose.FIND_EMAIL);return "find-email";
    }

    @GetMapping("/password/forgot") String forgot(Model model){model.addAttribute("smsConfigured",sms.isConfigured());return "forgot-password";}
    @PostMapping("/password/forgot") String authorizeReset(@RequestParam String email,@RequestParam String name,
        @RequestParam String birthDate,@RequestParam String phone,HttpSession session,Model model){
        model.addAttribute("smsConfigured",sms.isConfigured());SmsIdentity identity=identity(name,birthDate,phone,model);if(identity==null)return "forgot-password";
        if(!VerifiedPhoneProof.matches(session,VerificationPurpose.RESET_PASSWORD,identity)){model.addAttribute("error","이름·생년월일 입력 후 문자인증을 먼저 완료해 주세요.");return "forgot-password";}
        var member=members.findByEmailIgnoreCaseAndNameAndBirthDateAndPhone(email.trim().toLowerCase(),identity.name(),identity.birthDate(),identity.phone());
        VerifiedPhoneProof.consume(session,VerificationPurpose.RESET_PASSWORD);
        if(member.isEmpty()){model.addAttribute("error","입력한 회원 정보를 확인해 주세요.");return "forgot-password";}
        session.setAttribute(RESET_MEMBER,member.get().getId());
        session.setAttribute(RESET_EXPIRES,LocalDateTime.now().plusMinutes(10));
        return "redirect:/password/reset";
    }

    @GetMapping("/password/reset") String reset(HttpSession session,Model model){
        if(resetMemberId(session)==null)return "redirect:/password/forgot";
        if(!model.containsAttribute("passwordResetForm"))model.addAttribute("passwordResetForm",new PasswordResetForm());return "reset-password";
    }
    @PostMapping("/password/reset") String reset(@Valid @ModelAttribute PasswordResetForm passwordResetForm,BindingResult errors,HttpSession session,RedirectAttributes redirect){
        Long id=resetMemberId(session);if(id==null)return "redirect:/password/forgot";
        if(!passwordResetForm.getPassword().equals(passwordResetForm.getPasswordConfirm()))errors.rejectValue("passwordConfirm","mismatch","비밀번호 확인이 일치하지 않습니다.");
        if(errors.hasErrors())return "reset-password";
        var member=members.findById(id).orElseThrow();member.changePassword(encoder.encode(passwordResetForm.getPassword()));members.save(member);
        clearReset(session);redirect.addFlashAttribute("message","비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.");return "redirect:/login";
    }
    private Long resetMemberId(HttpSession session){
        Object memberId=session.getAttribute(RESET_MEMBER),expires=session.getAttribute(RESET_EXPIRES);
        if(memberId instanceof Long id&&expires instanceof LocalDateTime expiry&&expiry.isAfter(LocalDateTime.now()))return id;
        clearReset(session);return null;
    }
    private void clearReset(HttpSession session){session.removeAttribute(RESET_MEMBER);session.removeAttribute(RESET_EXPIRES);}
    private SmsIdentity identity(String name,String birthDate,String phone,Model model){try{return SmsIdentity.of(name,birthDate,phone);}catch(IllegalArgumentException e){model.addAttribute("error",e.getMessage());return null;}}
    private String mask(String email){int at=email.indexOf('@');if(at<=1)return "***"+email.substring(Math.max(0,at));return email.substring(0,2)+"***"+email.substring(at);}
}
