package com.jinmifood.shop.web;

import com.jinmifood.shop.domain.Member;
import com.jinmifood.shop.repository.MemberAdminActionRepository;
import com.jinmifood.shop.repository.MemberRepository;
import com.jinmifood.shop.service.MemberAdminService;
import com.jinmifood.shop.service.SocialMemberService;
import com.jinmifood.shop.config.ConfiguredAdminInitializer;
import com.jinmifood.shop.config.properties.AdminProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AdminMemberManagementTest {
    @Autowired MockMvc mvc;
    @Autowired MemberRepository members;
    @Autowired MemberAdminActionRepository actions;
    @Autowired MemberAdminService memberAdmin;
    @Autowired SocialMemberService socialMembers;
    @Autowired PasswordEncoder encoder;
    @Autowired AdminProperties adminProperties;

    @Test void configuredAdministratorCanLoginAndOpenAdministration() throws Exception {
        new ConfiguredAdminInitializer(members,encoder,adminProperties).run(null);
        mvc.perform(formLogin().user(adminProperties.getUsername()).password(adminProperties.getPassword()))
            .andExpect(authenticated().withRoles("MEMBER","ADMIN"));
    }

    @Test void memberListSearchAndDetailRenderForAdmin() throws Exception {
        var member=members.saveAndFlush(new Member("customer@example.com",encoder.encode("test1234"),"홍길동","01012345678"));
        mvc.perform(get("/admin/members").param("q","홍길동").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("customer@example.com")));
        mvc.perform(get("/admin/members/{id}",member.getId()).with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("회원 정보")))
            .andExpect(content().string(containsString("포인트 조정")));
    }

    @Test void socialSignupAppearsWithProviderInMemberAdministration() throws Exception {
        socialMembers.login("google",new SocialMemberService.SocialProfile("admin-list-google","social-list@example.com","구글 회원",true));
        mvc.perform(get("/admin/members").param("q","social-list@example.com").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("social-list@example.com")))
            .andExpect(content().string(containsString("GOOGLE")));
    }

    @Test void pointAdjustmentRequiresReasonAndCreatesAuditLog() {
        var member=members.saveAndFlush(new Member("points-admin@example.com","unused","포인트 회원","01011112222"));
        assertThatThrownBy(()->memberAdmin.adjustPoints(member.getId(),1000,"admin"," ")).isInstanceOf(IllegalArgumentException.class);
        memberAdmin.adjustPoints(member.getId(),1000,"admin","고객 보상");
        assertThat(members.findById(member.getId()).orElseThrow().getPoints()).isEqualTo(1000);
        var history=actions.findTop30ByMemberIdOrderByCreatedAtDesc(member.getId());
        assertThat(history).hasSize(1);assertThat(history.getFirst().getReason()).isEqualTo("고객 보상");
    }

    @Test void selfPrivilegeChangeAndLastAdminRemovalAreBlocked() {
        var self=new Member("owner@example.com","unused","소유자","01022223333");self.grantAdmin();members.saveAndFlush(self);
        assertThatThrownBy(()->memberAdmin.changeAdmin(self.getId(),false,self.getEmail(),"실수"))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("자기 계정");
        assertThatThrownBy(()->memberAdmin.changeAdmin(self.getId(),false,"configured-admin","권한 해제"))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("마지막 활성 관리자");
    }

    @Test void blockedMemberCannotLoginOrContinueAnExistingSession() throws Exception {
        var member=new Member("blocked@example.com",encoder.encode("test1234"),"차단 회원","01044445555");member.deactivate();members.saveAndFlush(member);
        mvc.perform(formLogin().user(member.getEmail()).password("test1234")).andExpect(redirectedUrl("/login?error"));
        mvc.perform(get("/mypage").with(user(member.getEmail()).roles("MEMBER")))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?disabled"));
    }

    @Test void ordinaryMemberCannotOpenMemberAdministration() throws Exception {
        mvc.perform(get("/admin/members").with(user("member@example.com").roles("MEMBER"))).andExpect(status().isForbidden());
    }
}
