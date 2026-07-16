package com.jinmifood.shop.web;

import com.jinmifood.shop.domain.Member;
import com.jinmifood.shop.domain.VerificationPurpose;
import com.jinmifood.shop.config.properties.SmsProperties;
import com.jinmifood.shop.config.properties.SocialLoginProperties;
import com.jinmifood.shop.service.SmsVerificationService;
import com.jinmifood.shop.service.MemberWithdrawalService;
import jakarta.servlet.http.HttpSession;
import com.jinmifood.shop.repository.*;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.PageRequest;

@Controller
public class MemberController {
    private final MemberRepository members; private final CustomerOrderRepository orders; private final PasswordEncoder encoder;
    private final SmsProperties sms;private final SocialLoginProperties social;private final MemberWithdrawalService withdrawals;
    public MemberController(MemberRepository members,CustomerOrderRepository orders,PasswordEncoder encoder,SmsProperties sms,
        SocialLoginProperties social,MemberWithdrawalService withdrawals){this.members=members;this.orders=orders;this.encoder=encoder;this.sms=sms;this.social=social;this.withdrawals=withdrawals;}

    @GetMapping("/login") String login(Model model){addLoginOptions(model);return "login";}
    @GetMapping("/register") String register(Model model){if(!model.containsAttribute("registrationForm"))model.addAttribute("registrationForm",new RegistrationForm());addLoginOptions(model);return "register";}
    @PostMapping("/register") String register(@Valid @ModelAttribute RegistrationForm registrationForm,BindingResult errors,RedirectAttributes redirect,HttpSession session,Model model){
        if(!registrationForm.getPassword().equals(registrationForm.getPasswordConfirm()))errors.rejectValue("passwordConfirm","mismatch","비밀번호 확인이 일치하지 않습니다.");
        if(registrationForm.getEmail()!=null&&members.existsByEmailIgnoreCase(registrationForm.getEmail()))errors.rejectValue("email","duplicate","이미 가입된 이메일입니다.");
        String phone="";try{phone=SmsVerificationService.normalizePhone(registrationForm.getPhone());}catch(IllegalArgumentException e){errors.rejectValue("phone","invalid",e.getMessage());}
        if(sms.isRequireVerification()&&!phone.isBlank()&&!VerifiedPhoneProof.matches(session,VerificationPurpose.REGISTER,phone))errors.rejectValue("phone","unverified","휴대전화 문자인증을 완료해 주세요.");
        if(errors.hasErrors()){addLoginOptions(model);return "register";}
        members.save(new Member(registrationForm.getEmail(),encoder.encode(registrationForm.getPassword()),registrationForm.getName().trim(),phone));
        VerifiedPhoneProof.consume(session,VerificationPurpose.REGISTER);
        redirect.addFlashAttribute("message","회원가입이 완료되었습니다. 로그인해 주세요.");return "redirect:/login";
    }
    @GetMapping("/mypage") String mypage(Authentication authentication,@RequestParam(defaultValue="0") int page,Model model){
        var member=members.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        model.addAttribute("member",member);
        model.addAttribute("orders",orders.findMemberOrders(member.getId(),PageRequest.of(Math.max(0,Math.min(page,100_000)),20)));
        model.addAttribute("socialSession",authentication instanceof OAuth2AuthenticationToken);
        return "store/mypage-page";
    }
    @PostMapping("/mypage/withdraw")
    String withdraw(@RequestParam(required=false)String currentPassword,@RequestParam String confirmation,
        Authentication authentication,HttpSession session,RedirectAttributes redirect){
        try{
            withdrawals.withdrawSelf(authentication.getName(),currentPassword,confirmation,authentication instanceof OAuth2AuthenticationToken);
            SecurityContextHolder.clearContext();session.invalidate();return "redirect:/?withdrawn";
        }catch(IllegalArgumentException|IllegalStateException|java.util.NoSuchElementException e){
            redirect.addFlashAttribute("withdrawalError",e.getMessage());return "redirect:/mypage#withdrawal";
        }
    }
    private void addLoginOptions(Model model){model.addAttribute("smsConfigured",sms.isConfigured());model.addAttribute("socialProviders",social.configuredProviders());}
}
