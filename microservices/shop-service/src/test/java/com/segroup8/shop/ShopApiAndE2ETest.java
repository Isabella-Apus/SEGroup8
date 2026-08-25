package com.segroup8.shop;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest @AutoConfigureMockMvc
class ShopApiAndE2ETest {
 @Autowired MockMvc mvc;@Autowired JdbcTemplate db;
 @BeforeEach void seed(){db.update("delete from shops");db.update("insert into shops(id,seller_id,name,announcement,status,decoration_template,decoration_json,updated_at) values(8,7,'旧店名','','OPEN','CLASSIC','{}',CURRENT_TIMESTAMP)");}
 @Test void uc08ViewSettingsDecorationE2EAndAllApis() throws Exception{
  mvc.perform(get("/api/shops/8")).andExpect(status().isOk()).andExpect(jsonPath("$.catalogAvailable").value(false));
  mvc.perform(get("/api/shops/seller/current").header("X-Seller-Id",7)).andExpect(status().isOk());
  mvc.perform(put("/api/shops/seller/current/settings").header("X-Seller-Id",7).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"第八组精品店\",\"announcement\":\"欢迎\",\"open\":true}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("第八组精品店"));
  mvc.perform(put("/api/shops/seller/current/decoration").header("X-Seller-Id",7).contentType(MediaType.APPLICATION_JSON).content("{\"template\":\"GRID\",\"contentJson\":\"{\\\"hero\\\":\\\"summer\\\"}\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.decorationTemplate").value("GRID"));
 }
 @Test void decorationRuleRejectsUnknownTemplate(){assertThatThrownBy(()->DecorationPolicy.validate("SCRIPT","{}" )).isInstanceOf(ShopException.class);}
}
