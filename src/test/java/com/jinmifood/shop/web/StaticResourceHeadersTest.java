package com.jinmifood.shop.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class StaticResourceHeadersTest {
    @Autowired MockMvc mvc;

    @Test void cssAndCatalogImagesArePubliclyCacheable() throws Exception {
        mvc.perform(get("/css/style.css")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=3600")))
                .andExpect(header().string("Cache-Control", containsString("public")));
        mvc.perform(get("/js/toss-payment.js")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=3600")))
                .andExpect(header().string("Cache-Control", containsString("public")));
        mvc.perform(get("/images/catalog/1746168229-crafted-detail.jpg")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=2592000")))
                .andExpect(header().string("Cache-Control", containsString("public")));
        mvc.perform(get("/images/hero/ivory-jjagtae-hero.webp")).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("image/webp")))
                .andExpect(header().string("Cache-Control", containsString("max-age=2592000")));
        var font = mvc.perform(get("/fonts/PretendardVariable-Jinmi.woff2")).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("font/woff2")))
                .andExpect(header().string("Cache-Control", containsString("max-age=2592000")))
                .andExpect(header().string("Cache-Control", containsString("public")))
                .andReturn();
        assertThat(font.getResponse().getContentAsByteArray().length).isLessThan(150_000);
    }

    @Test void storefrontResponsesDeclareKoreanLanguage() throws Exception {
        var response = mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(header().string("Content-Language", containsString("ko")))
                .andReturn().getResponse().getContentAsString();

        assertThat(response)
                .contains("rel=\"preload\" href=\"/fonts/PretendardVariable-Jinmi.woff2\"")
                .contains("/css/storefront-ivory.css?v=20260820-1");
        assertThat(response.indexOf("/css/storefront-ivory.css"))
                .isLessThan(response.indexOf("</head>"));
        assertThat(response.indexOf("/fonts/PretendardVariable-Jinmi.woff2"))
                .isLessThan(response.indexOf("</head>"));
    }
}
