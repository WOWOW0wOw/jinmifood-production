package com.jinmifood.shop.web;

import com.jinmifood.shop.domain.Member;
import com.jinmifood.shop.domain.VerificationPurpose;
import com.jinmifood.shop.repository.MemberRepository;
import com.jinmifood.shop.service.SocialMemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class SocialPhoneVerificationTest {
    @Autowired MockMvc mvc;
    @Autowired MemberRepository members;
    @Autowired SocialMemberService socialMembers;

    @Test void socialMemberWithoutPhoneCannotBypassVerification() throws Exception {
        Member member=social("social-gate","social-gate@example.com");

        mvc.perform(get("/products").with(socialLogin(member.getEmail())))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/social/phone"));
        mvc.perform(get("/social/phone").with(socialLogin(member.getEmail())))
                .andExpect(status().isOk()).andExpect(content().string(containsString("문자로 본인 확인")))
                .andExpect(content().string(containsString("data-purpose=\"SOCIAL_LOGIN\"")));
    }

    @Test void verifiedSmsProofCompletesSocialSignup() throws Exception {
        Member member=social("social-complete","social-complete@example.com");
        var identity=SmsIdentity.of("소셜회원",LocalDate.of(1992,3,4),"01012345678");
        var session=new MockHttpSession();
        VerifiedPhoneProof.store(session,VerificationPurpose.SOCIAL_LOGIN,identity);

        mvc.perform(post("/social/phone").session(session).with(socialLogin(member.getEmail())).with(csrf())
                        .param("name",identity.name()).param("birthDate","1992-03-04").param("phone","010-1234-5678"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));

        var saved=members.findById(member.getId()).orElseThrow();
        assertThat(saved.getName()).isEqualTo(identity.name());
        assertThat(saved.getBirthDate()).isEqualTo(identity.birthDate());
        assertThat(saved.getPhone()).isEqualTo(identity.phone());
        assertThat(VerifiedPhoneProof.matches(session,VerificationPurpose.SOCIAL_LOGIN,identity)).isFalse();
        mvc.perform(get("/products").with(socialLogin(member.getEmail()))).andExpect(status().isOk());
    }

    @Test void rejectsSubmissionWithoutSmsProof() throws Exception {
        Member member=social("social-unverified","social-unverified@example.com");

        mvc.perform(post("/social/phone").with(socialLogin(member.getEmail())).with(csrf())
                        .param("name","소셜회원").param("birthDate","1992-03-04").param("phone","01012345678"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("문자로 휴대전화 본인 확인을 완료해 주세요.")));
        assertThat(members.findById(member.getId()).orElseThrow().getPhone()).isBlank();
    }

    @Test void rejectsPhoneAlreadyUsedByAnotherMember() throws Exception {
        members.saveAndFlush(new Member("existing-phone@example.com","unused","기존 회원","01087654321"));
        Member social=social("social-duplicate","social-duplicate@example.com");
        var identity=SmsIdentity.of("소셜회원",LocalDate.of(1992,3,4),"01087654321");
        var session=new MockHttpSession();
        VerifiedPhoneProof.store(session,VerificationPurpose.SOCIAL_LOGIN,identity);

        mvc.perform(post("/social/phone").session(session).with(socialLogin(social.getEmail())).with(csrf())
                        .param("name",identity.name()).param("birthDate","1992-03-04").param("phone",identity.phone()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("이미 다른 회원이 사용 중인 휴대전화번호입니다.")));
        assertThat(members.findById(social.getId()).orElseThrow().getPhone()).isBlank();
    }

    @Test void ordinaryPasswordMemberIsNotForcedIntoSocialVerification() throws Exception {
        var member=members.saveAndFlush(new Member("password-member@example.com","unused","일반 회원",""));
        mvc.perform(get("/products").with(user(member.getEmail()).roles("MEMBER"))).andExpect(status().isOk());
    }

    @Test void smsProofCannotBeReusedWithChangedNameOrBirthDate() {
        var verified=SmsIdentity.of("김진미",LocalDate.of(1990,1,2),"01012345678");
        var session=new MockHttpSession();
        VerifiedPhoneProof.store(session,VerificationPurpose.SOCIAL_LOGIN,verified);

        assertThat(VerifiedPhoneProof.matches(session,VerificationPurpose.SOCIAL_LOGIN,
                SmsIdentity.of("다른이름",verified.birthDate(),verified.phone()))).isFalse();
        assertThat(VerifiedPhoneProof.matches(session,VerificationPurpose.SOCIAL_LOGIN,
                SmsIdentity.of(verified.name(),LocalDate.of(1990,1,3),verified.phone()))).isFalse();
    }

    private Member social(String providerId,String email){
        return socialMembers.login("google",new SocialMemberService.SocialProfile(providerId,email,"소셜 회원",true));
    }

    private RequestPostProcessor socialLogin(String email){
        var principal=new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_MEMBER")),Map.of("memberEmail",email),"memberEmail");
        return oauth2Login().oauth2User(principal);
    }
}
