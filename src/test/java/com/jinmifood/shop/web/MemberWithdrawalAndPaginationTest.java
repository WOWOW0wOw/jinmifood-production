package com.jinmifood.shop.web;

import com.jinmifood.shop.domain.CustomerOrder;
import com.jinmifood.shop.domain.Member;
import com.jinmifood.shop.domain.SocialAccount;
import com.jinmifood.shop.repository.CustomerOrderRepository;
import com.jinmifood.shop.repository.MemberAdminActionRepository;
import com.jinmifood.shop.repository.MemberRepository;
import com.jinmifood.shop.repository.SocialAccountRepository;
import com.jinmifood.shop.service.MemberWithdrawalService;
import com.jinmifood.shop.service.MemberAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class MemberWithdrawalAndPaginationTest {
    @Autowired MockMvc mvc;
    @Autowired MemberRepository members;
    @Autowired SocialAccountRepository socialAccounts;
    @Autowired CustomerOrderRepository orders;
    @Autowired MemberAdminActionRepository actions;
    @Autowired MemberWithdrawalService withdrawals;
    @Autowired MemberAdminService memberAdmin;
    @Autowired PasswordEncoder encoder;

    @Test void catalogAndAdminListsUseServerSidePagesAndClampNegativePage() throws Exception {
        mvc.perform(get("/products").param("page","-7"))
            .andExpect(status().isOk()).andExpect(model().attributeExists("products"))
            .andExpect(result->assertThat(((Page<?>)result.getModelAndView().getModel().get("products")).getNumber()).isZero());
        mvc.perform(get("/admin/products").param("page","-1").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(result->assertThat(((Page<?>)result.getModelAndView().getModel().get("products")).getSize()).isEqualTo(20));
        mvc.perform(get("/admin/orders").param("page","-1").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(result->assertThat(((Page<?>)result.getModelAndView().getModel().get("orders")).getSize()).isEqualTo(30));
    }

    @Test void productSearchKeepsFilterOnPaginationLinks() throws Exception {
        mvc.perform(get("/products").param("q","진미").param("page","0"))
            .andExpect(status().isOk()).andExpect(model().attribute("query","진미"));
    }

    @Test void passwordMemberCanWithdrawAndOrderHistoryRemainsLinked() throws Exception {
        var member=members.saveAndFlush(new Member("leave@example.com",encoder.encode("correct123"),"탈퇴 대상","01012341234"));
        var order=orders.saveAndFlush(new CustomerOrder("JM-WITHDRAW-1","탈퇴 대상","01012341234","leave@example.com","04524","서울시 중구",null,null,10000,3500,member,0));

        withdrawals.withdrawSelf(member.getEmail(),"correct123",MemberWithdrawalService.CONFIRMATION,false);

        var withdrawn=members.findById(member.getId()).orElseThrow();
        assertThat(withdrawn.isWithdrawn()).isTrue();
        assertThat(withdrawn.isActive()).isFalse();
        assertThat(withdrawn.getEmail()).endsWith("@deleted.invalid");
        assertThat(withdrawn.getPhone()).isBlank();
        assertThat(orders.findById(order.getId()).orElseThrow().getMember().getId()).isEqualTo(member.getId());
    }

    @Test void wrongPasswordOrConfirmationCannotWithdraw() {
        var member=members.saveAndFlush(new Member("stay@example.com",encoder.encode("correct123"),"유지 회원","01022223333"));
        assertThatThrownBy(()->withdrawals.withdrawSelf(member.getEmail(),"wrong","탈퇴합니다",false))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("비밀번호");
        assertThatThrownBy(()->withdrawals.withdrawSelf(member.getEmail(),"correct123","탈퇴할게요",false))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("확인 문구");
        assertThat(members.findById(member.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test void socialWithdrawalRemovesProviderConnection() {
        var member=members.saveAndFlush(new Member("social-leave@example.com",encoder.encode("unused"),"소셜 회원","01033334444"));
        socialAccounts.saveAndFlush(new SocialAccount("google","provider-user-1",member));
        withdrawals.withdrawSelf(member.getEmail(),null,"탈퇴합니다",true);
        assertThat(socialAccounts.existsByMemberId(member.getId())).isFalse();
    }

    @Test void administratorCanWithdrawAnotherMemberWithAuditButNotSelf() {
        var admin=new Member("admin-withdraw@example.com",encoder.encode("admin1234"),"관리자","01044445555");admin.grantAdmin();members.saveAndFlush(admin);
        var target=members.saveAndFlush(new Member("target-withdraw@example.com",encoder.encode("member1234"),"대상 회원","01055556666"));
        withdrawals.withdrawByAdmin(target.getId(),admin.getEmail(),"고객 요청 확인");
        assertThat(members.findById(target.getId()).orElseThrow().isWithdrawn()).isTrue();
        assertThat(actions.findTop30ByMemberIdOrderByCreatedAtDesc(target.getId())).anyMatch(a->a.getAction().equals("WITHDRAWAL"));
        assertThatThrownBy(()->memberAdmin.changeActive(target.getId(),true,admin.getEmail(),"복구 시도"))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("탈퇴한 회원");
        assertThatThrownBy(()->withdrawals.withdrawByAdmin(admin.getId(),admin.getEmail(),"실수"))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("자기 계정");
    }

    @Test void withdrawalEndpointRequiresCsrfAndLogsMemberOut() throws Exception {
        var member=members.saveAndFlush(new Member("web-leave@example.com",encoder.encode("correct123"),"웹 회원","01066667777"));
        mvc.perform(post("/mypage/withdraw").with(user(member.getEmail()).roles("MEMBER"))
                .param("currentPassword","correct123").param("confirmation","탈퇴합니다"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/mypage/withdraw").with(user(member.getEmail()).roles("MEMBER")).with(csrf())
                .param("currentPassword","correct123").param("confirmation","탈퇴합니다"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/?withdrawn"));
        assertThat(members.findById(member.getId()).orElseThrow().isWithdrawn()).isTrue();
    }
}
