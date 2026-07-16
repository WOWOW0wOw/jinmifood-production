package com.jinmifood.shop.web;

import com.jinmifood.shop.config.properties.SmsProperties;
import com.jinmifood.shop.domain.VerificationPurpose;
import com.jinmifood.shop.repository.MemberRepository;
import com.jinmifood.shop.service.SmsVerificationService;
import com.jinmifood.shop.service.SocialMemberService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SocialPhoneController {
    private final MemberRepository members;
    private final SocialMemberService socialMembers;
    private final SmsProperties sms;

    public SocialPhoneController(MemberRepository members,SocialMemberService socialMembers,SmsProperties sms){
        this.members=members;this.socialMembers=socialMembers;this.sms=sms;
    }

    @GetMapping("/social/phone")
    String form(Authentication authentication,Model model){
        if(!(authentication instanceof OAuth2AuthenticationToken))return "redirect:/";
        var member=members.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        if(!member.getPhone().isBlank())return "redirect:/";
        model.addAttribute("smsConfigured",sms.isConfigured());
        return "social-phone";
    }

    @PostMapping("/social/phone")
    String complete(@RequestParam String phone,Authentication authentication,HttpSession session,Model model,RedirectAttributes redirect){
        if(!(authentication instanceof OAuth2AuthenticationToken))return "redirect:/";
        model.addAttribute("smsConfigured",sms.isConfigured());
        String normalized;
        try{normalized=SmsVerificationService.normalizePhone(phone);}
        catch(IllegalArgumentException e){model.addAttribute("error",e.getMessage());return "social-phone";}
        if(!VerifiedPhoneProof.matches(session,VerificationPurpose.SOCIAL_LOGIN,normalized)){
            model.addAttribute("error","문자로 휴대전화 본인 확인을 완료해 주세요.");return "social-phone";
        }
        try{socialMembers.completeVerifiedPhone(authentication.getName(),normalized);}
        catch(IllegalArgumentException|IllegalStateException e){model.addAttribute("error",e.getMessage());return "social-phone";}
        VerifiedPhoneProof.consume(session,VerificationPurpose.SOCIAL_LOGIN);
        redirect.addFlashAttribute("message","휴대전화 본인 확인이 완료되었습니다.");
        return "redirect:/";
    }
}
