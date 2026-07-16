package com.jinmifood.shop.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PolicyPageTest {
    @Autowired MockMvc mvc;

    @Test void productionPolicyContainsRequiredBusinessPrivacyAndRefundTerms() throws Exception {
        mvc.perform(get("/policies"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("807-56-00747")))
                .andExpect(content().string(containsString("계약 또는 청약철회, 대금결제 및 재화 공급 기록: 5년")))
                .andExpect(content().string(containsString("왕복 7,000원")))
                .andExpect(content().string(containsString("개인정보 보호책임자")))
                .andExpect(content().string(not(containsString("초안"))));
    }
}
