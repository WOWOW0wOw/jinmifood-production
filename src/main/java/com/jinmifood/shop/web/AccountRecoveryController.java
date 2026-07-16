package com.jinmifood.shop.web;

import com.jinmifood.shop.config.properties.SmsProperties;
import com.jinmifood.shop.domain.VerificationPurpose;
import com.jinmifood.shop.repository.MemberRepository;
import com.jinmifood.shop.service.SmsVerificationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountRecoveryController {
    private static final String RESET_MEMBER="password-reset-member";
    private final MemberRepository members;private final PasswordEncoder encoder;private final SmsProperties sms;
    public AccountRecoveryController(MemberRepository members,PasswordEncoder encoder,SmsProperties sms){this.members=members;this.encoder=encoder;this.sms=sms;}

    @GetMapping("/account/find-email") String findEmail(Model model){model.addAttribute("smsConfigured",sms.isConfigured());return "find-email";}
    @PostMapping("/account/find-email") String findEmailResult(@RequestParam String name,@RequestParam String phone,HttpSession session,Model model){
        String normalized=normalize(phone);model.addAttribute("smsConfigured",sms.isConfigured());
        if(!VerifiedPhoneProof.matches(session,VerificationPurpose.FIND_EMAIL,normalized)){model.addAttribute("error","문자인증을 먼저 완료해 주세요.");return "find-email";}
        var emails=members.findByNameAndPhoneOrderByCreatedAtAsc(name.trim(),normalized).stream().map(m->mask(m.getEmail())).toList();
        model.addAttribute("emails",emails);VerifiedPhoneProof.consume(session,VerificationPurpose.FIND_EMAIL);return "find-email";
    }

    @GetMapping("/password/forgot") String forgot(Model model){model.addAttribute("smsConfigured",sms.isConfigured());return "forgot-password";}
    @PostMapping("/password/forgot") String authorizeReset(@RequestParam String email,@RequestParam String phone,HttpSession session,Model model){
        String normalized=normalize(phone);model.addAttribute("smsConfigured",sms.isConfigured());
        if(!VerifiedPhoneProof.matches(session,VerificationPurpose.RESET_PASSWORD,normalized)){model.addAttribute("error","문자인증을 먼저 완료해 주세요.");return "forgot-password";}
        var member=members.findByEmailIgnoreCaseAndPhone(email.trim().toLowerCase(),normalized);
        VerifiedPhoneProof.consume(session,VerificationPurpose.RESET_PASSWORD);
        if(member.isEmpty()){model.addAttribute("error","입력한 회원 정보를 확인해 주세요.");return "forgot-password";}
        session.setAttribute(RESET_MEMBER,member.get().getId());return "redirect:/password/reset";
    }

    @GetMapping("/password/reset") String reset(HttpSession session,Model model){
        if(!(session.getAttribute(RESET_MEMBER) instanceof Long))return "redirect:/password/forgot";
        if(!model.containsAttribute("passwordResetForm"))model.addAttribute("passwordResetForm",new PasswordResetForm());return "reset-password";
    }
    @PostMapping("/password/reset") String reset(@Valid @ModelAttribute PasswordResetForm passwordResetForm,BindingResult errors,HttpSession session,RedirectAttributes redirect){
        Object memberId=session.getAttribute(RESET_MEMBER);if(!(memberId instanceof Long id))return "redirect:/password/forgot";
        if(!passwordResetForm.getPassword().equals(passwordResetForm.getPasswordConfirm()))errors.rejectValue("passwordConfirm","mismatch","비밀번호 확인이 일치하지 않습니다.");
        if(errors.hasErrors())return "reset-password";
        var member=members.findById(id).orElseThrow();member.changePassword(encoder.encode(passwordResetForm.getPassword()));members.save(member);
        session.removeAttribute(RESET_MEMBER);redirect.addFlashAttribute("message","비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.");return "redirect:/login";
    }
    private String normalize(String phone){try{return SmsVerificationService.normalizePhone(phone);}catch(IllegalArgumentException e){return "";}}
    private String mask(String email){int at=email.indexOf('@');if(at<=1)return "***"+email.substring(Math.max(0,at));return email.substring(0,2)+"***"+email.substring(at);}
}
