package com.jinmifood.shop.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
        mvc.perform(get("/images/catalog/1746168229-ai-intro.jpg")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=2592000")))
                .andExpect(header().string("Cache-Control", containsString("public")));
        mvc.perform(get("/fonts/PretendardVariable.woff2")).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("font/woff2")))
                .andExpect(header().string("Cache-Control", containsString("max-age=2592000")))
                .andExpect(header().string("Cache-Control", containsString("public")));
    }

    @Test void storefrontResponsesDeclareKoreanLanguage() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(header().string("Content-Language", containsString("ko")));
    }
}
