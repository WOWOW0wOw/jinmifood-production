package com.jinmifood.shop.web;

import com.jinmifood.shop.repository.ProductRepository;
import com.jinmifood.shop.repository.MemberRepository;
import com.jinmifood.shop.domain.*;
import com.jinmifood.shop.service.ShopService;
import com.jinmifood.shop.service.SocialMemberService;
import com.jinmifood.shop.repository.SocialAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class StoreControllerTest {
    @TempDir static Path uploadDir;
    @DynamicPropertySource static void uploadProperties(DynamicPropertyRegistry registry){registry.add("app.upload.dir",uploadDir::toString);}
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired MemberRepository members;
    @Autowired ShopService shop;
    @Autowired PasswordEncoder encoder;
    @Autowired SocialMemberService socialMembers;
    @Autowired SocialAccountRepository socialAccounts;

    @Test void homeIsPublic() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString("진미푸드 추천상품")));
    }

    @Test void anonymousHomeViewDoesNotAllocateAServerSession() throws Exception {
        var result=mvc.perform(get("/")).andExpect(status().isOk()).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test void loginShowsConfiguredSocialProvidersAndRecoveryLinks() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Google로 계속하기")))
            .andExpect(content().string(containsString("카카오로 계속하기")))
            .andExpect(content().string(containsString("네이버로 계속하기")))
            .andExpect(content().string(containsString("아이디 찾기")))
            .andExpect(content().string(containsString("비밀번호 재설정")));
    }

    @Test void securityHeadersProtectPublicPages() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")));
    }

    @Test void productShowsRequiredFoodInformation() throws Exception {
        mvc.perform(get("/products/meoktae-1kg")).andExpect(status().isOk())
            .andExpect(content().string(containsString("상품 필수정보")))
            .andExpect(content().string(containsString("원산지")));
    }

    @Test void orderLookupIsPublic() throws Exception {
        mvc.perform(get("/order-lookup")).andExpect(status().isOk()).andExpect(content().string(containsString("비회원 주문조회")));
    }

    @Test void unsafeCartReturnUrlIsRejected() throws Exception {
        var product=products.findByActiveTrueOrderByCreatedAtDesc().getFirst();
        mvc.perform(post("/cart/add").with(csrf()).param("productId",product.getId().toString())
            .param("quantity","1").param("returnUrl","//evil.example"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/cart"));
    }

    @Test void adminRequiresLogin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
    }

    @Test void actuatorDiscoveryAndSensitiveEndpointsAreDenied() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/actuator/env")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/actuator").with(user("member").roles("MEMBER"))).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/env").with(user("member").roles("MEMBER"))).andExpect(status().isForbidden());
    }

    @Test void adminProductSaveAcceptsCsrfWithoutTouchingImage() throws Exception {
        var product=products.findByActiveTrueOrderByCreatedAtDesc().getFirst();
        String previousImage=product.getImageUrl();
        mvc.perform(post("/admin/products/save").with(user("admin").roles("ADMIN")).with(csrf())
                .param("id",product.getId().toString()).param("name",product.getName()).param("slug",product.getSlug())
                .param("categoryId",product.getCategory().getId().toString()).param("price",product.getPrice().toString())
                .param("stock",product.getStock().toString()).param("active","true"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/products"));
        assertThat(products.findById(product.getId()).orElseThrow().getImageUrl()).isEqualTo(previousImage);
    }

    @Test void imageReplacementDoesNotCorruptProductInformation() throws Exception {
        var product=products.findByActiveTrueOrderByCreatedAtDesc().getFirst();
        product.setDetailImageUrl("/images/catalog/1746168600-detail.jpg");
        products.saveAndFlush(product);
        var output=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"jpg",output);byte[] jpeg=output.toByteArray();
        var image=new MockMultipartFile("imageFile","new.jpg","image/jpeg",jpeg);
        mvc.perform(multipart("/admin/products/"+product.getId()+"/image").file(image).with(user("admin").roles("ADMIN")).with(csrf()))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/products/"+product.getId()+"/edit"));
        var saved=products.findById(product.getId()).orElseThrow();
        assertThat(saved.getName()).isEqualTo(product.getName());assertThat(saved.getPrice()).isEqualTo(product.getPrice());
        assertThat(saved.getStock()).isEqualTo(product.getStock());assertThat(saved.getOrigin()).isEqualTo(product.getOrigin());
        assertThat(saved.getManufacturer()).isEqualTo(product.getManufacturer());
        assertThat(saved.getImageUrl()).startsWith("/uploads/product-").endsWith(".jpg");
        assertThat(saved.getDetailImageUrl()).isEqualTo("/images/catalog/1746168600-detail.jpg");
    }

    @Test void memberCanRegisterLoginAndOpenMyPage() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("email","member@example.com").param("name","진미회원")
                .param("birthDate","1990-01-02").param("phone","010-1234-5678")
                .param("password","test1234").param("passwordConfirm","test1234"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(formLogin().user("member@example.com").password("test1234")).andExpect(authenticated().withRoles("MEMBER"));
        mvc.perform(get("/mypage").with(user("member@example.com").roles("MEMBER")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("보유 포인트")));
    }

    @Test void verifiedMemberCanResetPassword() throws Exception {
        var member=members.saveAndFlush(new Member("reset@example.com",encoder.encode("oldpass123"),"재설정회원",LocalDate.of(1990,1,2),"01012345678"));
        var identity=SmsIdentity.of(member.getName(),member.getBirthDate(),member.getPhone());
        var session=new MockHttpSession();VerifiedPhoneProof.store(session,VerificationPurpose.RESET_PASSWORD,identity);
        mvc.perform(post("/password/forgot").session(session).with(csrf()).param("email",member.getEmail())
                .param("name",member.getName()).param("birthDate","1990-01-02").param("phone","010-1234-5678"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/password/reset"));
        mvc.perform(post("/password/reset").session(session).with(csrf()).param("password","newpass123").param("passwordConfirm","newpass123"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        assertThat(encoder.matches("newpass123",members.findById(member.getId()).orElseThrow().getPasswordHash())).isTrue();
    }

    @Test void expiredPasswordResetAuthorizationCannotBeUsed() throws Exception {
        var session=new MockHttpSession();
        session.setAttribute("password-reset-member",1L);
        session.setAttribute("password-reset-expires",LocalDateTime.now().minusSeconds(1));

        mvc.perform(get("/password/reset").session(session))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/password/forgot"));
        assertThat(session.getAttribute("password-reset-member")).isNull();
        assertThat(session.getAttribute("password-reset-expires")).isNull();
    }

    @Test void verifiedMemberCanFindMaskedEmail() throws Exception {
        var member=members.saveAndFlush(new Member("findme@example.com",encoder.encode("test1234"),"아이디회원",LocalDate.of(1991,2,3),"01087654321"));
        var identity=SmsIdentity.of(member.getName(),member.getBirthDate(),member.getPhone());
        var session=new MockHttpSession();VerifiedPhoneProof.store(session,VerificationPurpose.FIND_EMAIL,identity);
        mvc.perform(post("/account/find-email").session(session).with(csrf()).param("name",member.getName())
                .param("birthDate","1991-02-03").param("phone","010-8765-4321"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("fi***@example.com")));
    }

    @Test void adminNavigationIsVisibleOnlyToAdministrators() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(containsString("href=\"/admin\""))));
        mvc.perform(get("/").with(user("member").roles("MEMBER"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("href=\"/admin\""))));
        mvc.perform(get("/").with(user("admin").roles("MEMBER","ADMIN"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/admin\"")));
    }

    @Test void socialProviderIdCreatesOnlyOneMemberAccount() {
        var profile=new SocialMemberService.SocialProfile("naver-user-1","social@example.com","소셜회원",false);
        var first=socialMembers.login("naver",profile);var second=socialMembers.login("naver",profile);
        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(socialAccounts.findByProviderAndProviderUserId("naver","naver-user-1")).isPresent();
    }

    @Test void memberDefaultAddressIsPrefilledAtCheckout() throws Exception {
        var member=new Member("address@example.com",encoder.encode("test1234"),"주소회원","01033334444");
        member.updateDefaultAddress("06236","서울특별시 강남구 테헤란로","101호");members.saveAndFlush(member);
        var product=products.findByActiveTrueOrderByCreatedAtDesc().getFirst();var cart=new Cart();cart.add(product.getId(),1);var session=new MockHttpSession();session.setAttribute("cart",cart);
        mvc.perform(get("/checkout").session(session).with(user(member.getEmail()).roles("MEMBER")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("06236")))
            .andExpect(content().string(containsString("서울특별시 강남구 테헤란로"))).andExpect(content().string(containsString("101호")));
    }

    @Test void checkoutCanRememberANewDefaultAddress() throws Exception {
        var member=members.saveAndFlush(new Member("new-address@example.com",encoder.encode("test1234"),"배송회원","01055556666"));
        var product=products.findByActiveTrueOrderByCreatedAtDesc().getFirst();var cart=new Cart();cart.add(product.getId(),1);var session=new MockHttpSession();session.setAttribute("cart",cart);
        mvc.perform(post("/checkout").session(session).with(user(member.getEmail()).roles("MEMBER")).with(csrf())
                .param("customerName","배송회원").param("phone","010-5555-6666").param("email",member.getEmail())
                .param("postalCode","04524").param("address","서울특별시 중구 세종대로").param("addressDetail","202호")
                .param("rememberAddress","true").param("agreed","true").param("pointsToUse","0"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("/payments/toss/request/**"));
        var saved=members.findById(member.getId()).orElseThrow();assertThat(saved.getPostalCode()).isEqualTo("04524");assertThat(saved.getAddress()).isEqualTo("서울특별시 중구 세종대로");assertThat(saved.getAddressDetail()).isEqualTo("202호");
    }

    @Test void adminMemberCanLoginAndOpenAdminPage() throws Exception {
        var member=new Member("member-admin@example.com",encoder.encode("test1234"),"관리회원","010-3333-4444");
        member.grantAdmin();
        members.saveAndFlush(member);

        var login=mvc.perform(formLogin().user(member.getEmail()).password("test1234"))
            .andExpect(authenticated().withRoles("MEMBER","ADMIN"))
            .andReturn();
        var session=(MockHttpSession)login.getRequest().getSession(false);
        mvc.perform(get("/admin").session(session))
            .andExpect(status().isOk());
    }

    @Test void memberUsesPointsAndEarnsTenPercentWhenPaid() {
        var member=members.save(new Member("points@example.com","unused","포인트회원","010-2222-3333"));member.addPoints(5000);
        var product=products.findByActiveTrueOrderByCreatedAtDesc().getFirst();var cart=new Cart();cart.add(product.getId(),1);
        var form=new CheckoutForm();form.setCustomerName(member.getName());form.setPhone(member.getPhone());form.setEmail(member.getEmail());
        form.setPostalCode("07200");form.setAddress("서울시 영등포구");form.setPointsToUse(1000);form.setAgreed(true);
        var order=shop.placeOrder(cart,form,member.getEmail());
        assertThat(member.getPoints()).isEqualTo(4000);assertThat(order.getTotalAmount()).isEqualTo(product.getPrice()+order.getShippingFee()-1000);
        shop.changeOrderStatus(order.getId(),OrderStatus.PAID);
        int expectedEarned=(product.getPrice()-1000)/10;
        assertThat(order.getPointsEarned()).isEqualTo(expectedEarned);assertThat(member.getPoints()).isEqualTo(4000+expectedEarned);
        shop.changeOrderStatus(order.getId(),OrderStatus.CANCELLED);
        assertThat(member.getPoints()).isEqualTo(5000);assertThat(order.getPointsEarned()).isZero();
    }
}
