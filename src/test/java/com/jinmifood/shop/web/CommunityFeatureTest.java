package com.jinmifood.shop.web;

import com.jinmifood.shop.domain.*;
import com.jinmifood.shop.repository.*;
import com.jinmifood.shop.service.ShopService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CommunityFeatureTest {
    @Autowired MockMvc mvc; @Autowired ProductRepository products; @Autowired MemberRepository members;
    @Autowired ProductReviewRepository reviews; @Autowired ProductInquiryRepository inquiries; @Autowired NoticeRepository notices;
    @Autowired CustomerOrderRepository orders; @Autowired ShopService shop; @Autowired PasswordEncoder encoder;

    private Product product; private Member buyer; private Member other;
    @BeforeEach void setUp(){
        product=products.findByActiveTrueOrderByCreatedAtDesc().getFirst();
        buyer=members.saveAndFlush(new Member("review-buyer@example.com",encoder.encode("test1234"),"구매회원","01011112222"));
        other=members.saveAndFlush(new Member("review-other@example.com",encoder.encode("test1234"),"일반회원","01033334444"));
    }

    @Test void productPageShowsReviewAndInquirySections() throws Exception {
        mvc.perform(get("/products/"+product.getSlug())).andExpect(status().isOk())
            .andExpect(content().string(containsString("구매 리뷰"))).andExpect(content().string(containsString("상품 문의")));
    }
    @Test void anonymousCannotWriteReviewOrInquiry() throws Exception {
        mvc.perform(post("/products/"+product.getId()+"/reviews").with(csrf()).param("rating","5").param("content","좋아요"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(post("/products/"+product.getId()+"/inquiries").with(csrf()).param("title","질문").param("content","내용"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
    }
    @Test void nonBuyerCannotWriteReview() throws Exception {
        mvc.perform(post("/products/"+product.getId()+"/reviews").with(user(other.getEmail()).roles("MEMBER")).with(csrf())
            .param("rating","5").param("content","구매하지 않은 리뷰"))
            .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("message",containsString("구매 회원")));
        assertThat(reviews.countByProductId(product.getId())).isZero();
    }
    @Test void paidBuyerCanWriteOnlyOneReviewAndRatingIsDisplayed() throws Exception {
        markPurchased();
        mvc.perform(post("/products/"+product.getId()+"/reviews").with(user(buyer.getEmail()).roles("MEMBER")).with(csrf())
            .param("rating","5").param("content","정말 맛있습니다."))
            .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("message","리뷰를 등록했습니다."));
        mvc.perform(post("/products/"+product.getId()+"/reviews").with(user(buyer.getEmail()).roles("MEMBER")).with(csrf())
            .param("rating","4").param("content","중복 리뷰"))
            .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("message",containsString("이미 리뷰")));
        assertThat(reviews.countByProductId(product.getId())).isEqualTo(1);
        mvc.perform(get("/products/"+product.getSlug())).andExpect(status().isOk())
            .andExpect(content().string(containsString("정말 맛있습니다."))).andExpect(content().string(containsString("5.0")));
    }
    @Test void invalidRatingAndOversizedTextAreRejected() throws Exception {
        markPurchased();
        mvc.perform(post("/products/"+product.getId()+"/reviews").with(user(buyer.getEmail()).roles("MEMBER")).with(csrf())
            .param("rating","6").param("content","리뷰"))
            .andExpect(flash().attribute("message",containsString("1점부터 5점")));
        mvc.perform(post("/products/"+product.getId()+"/inquiries").with(user(buyer.getEmail()).roles("MEMBER")).with(csrf())
            .param("title","제목").param("content","가".repeat(2001)))
            .andExpect(flash().attribute("message",containsString("2,000자")));
    }
    @Test void memberCanWriteInquiryAndHtmlIsEscaped() throws Exception {
        mvc.perform(post("/products/"+product.getId()+"/inquiries").with(user(other.getEmail()).roles("MEMBER")).with(csrf())
            .param("title","<script>alert(1)</script>").param("content","<img src=x onerror=alert(1)>"))
            .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("message","상품 문의를 등록했습니다."));
        mvc.perform(get("/products/"+product.getSlug())).andExpect(status().isOk())
            .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
            .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
    }
    @Test void otherMemberCannotDeleteInquiryButAdminCanAnswer() throws Exception {
        mvc.perform(post("/products/"+product.getId()+"/inquiries").with(user(buyer.getEmail()).roles("MEMBER")).with(csrf()).param("title","배송 문의").param("content","언제 오나요?"));
        var inquiry=inquiries.findAll().getFirst();
        mvc.perform(post("/products/"+product.getId()+"/inquiries/"+inquiry.getId()+"/delete").with(user(other.getEmail()).roles("MEMBER")).with(csrf()))
            .andExpect(flash().attribute("message",containsString("본인이 작성한")));
        assertThat(inquiries.findById(inquiry.getId())).isPresent();
        mvc.perform(post("/admin/inquiries/"+inquiry.getId()+"/answer").with(user("admin@example.com").roles("ADMIN")).with(csrf()).param("answer","내일 출고됩니다."))
            .andExpect(status().is3xxRedirection());
        assertThat(inquiries.findById(inquiry.getId()).orElseThrow().getAnswer()).isEqualTo("내일 출고됩니다.");
    }
    @Test void noticesArePublicAndOnlyAdminCanManageThem() throws Exception {
        mvc.perform(get("/community")).andExpect(status().isOk()).andExpect(content().string(containsString("고객센터")));
        mvc.perform(get("/community/notices")).andExpect(status().isOk());
        mvc.perform(post("/admin/notices/save").with(user(other.getEmail()).roles("MEMBER")).with(csrf()).param("title","불가").param("content","불가"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/admin/notices/save").with(user("admin@example.com").roles("ADMIN")).with(csrf())
            .param("title","배송 안내").param("content","정상 배송합니다.").param("pinned","true"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/notices"));
        var notice=notices.findAll().getFirst();
        mvc.perform(get("/community/notices/"+notice.getId())).andExpect(status().isOk())
            .andExpect(content().string(containsString("배송 안내"))).andExpect(content().string(containsString("정상 배송합니다.")));
    }
    @Test void communityPagesAndAdminListsAreResponsiveEndpoints() throws Exception {
        mvc.perform(get("/community/customer-center")).andExpect(status().isOk()).andExpect(content().string(containsString("02-6397-6686")));
        mvc.perform(get("/admin/inquiries").with(user("admin@example.com").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/admin/notices").with(user("admin@example.com").roles("ADMIN"))).andExpect(status().isOk());
    }
    private void markPurchased(){
        var cart=new Cart();cart.add(product.getId(),1);var form=new CheckoutForm();form.setCustomerName(buyer.getName());form.setPhone(buyer.getPhone());
        form.setEmail(buyer.getEmail());form.setPostalCode("07200");form.setAddress("서울시 영등포구");form.setPointsToUse(0);form.setAgreed(true);
        var order=shop.placeOrder(cart,form,buyer.getEmail());order.markPaymentPaid("test_payment_"+order.getId(),"카드",LocalDateTime.now(),null);order.changeStatus(OrderStatus.PAID);orders.saveAndFlush(order);
    }
}
