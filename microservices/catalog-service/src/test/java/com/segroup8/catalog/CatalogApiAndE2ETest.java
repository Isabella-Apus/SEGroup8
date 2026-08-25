package com.segroup8.catalog;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest @AutoConfigureMockMvc
class CatalogApiAndE2ETest {
    @Autowired MockMvc mvc; @Autowired JdbcTemplate db;
    @BeforeEach void seed(){db.update("delete from integration_outbox");db.update("delete from products");db.update("insert into products(id,seller_id,shop_id,name,description,category,price,stock,status,updated_at) values(1,7,8,'Java 图书','微服务实践','BOOK',59.00,10,'ON_SALE',CURRENT_TIMESTAMP)");}

    @Test void allPublicApisAndUc06SearchDetail() throws Exception {
        mvc.perform(get("/api/catalog/products").param("keyword","Java").param("category","BOOK").param("minPrice","50").param("maxPrice","60"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
        mvc.perform(get("/api/catalog/products/1")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Java 图书"));
        mvc.perform(get("/api/catalog/seller/products").header("X-Seller-Id",7)).andExpect(status().isOk()).andExpect(jsonPath("$[0].shopId").value(8));
    }

    @Test void uc07LifecycleE2EAndWriteApis() throws Exception {
        String body="{\"shopId\":8,\"name\":\"新商品\",\"description\":\"安全商品\",\"category\":\"DIGITAL\",\"price\":99.00,\"stock\":3}";
        String created=mvc.perform(post("/api/catalog/seller/products").header("X-Seller-Id",7).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DRAFT")).andReturn().getResponse().getContentAsString();
        long id=new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).get("id").asLong();
        mvc.perform(put("/api/catalog/seller/products/{id}",id).header("X-Seller-Id",7).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mvc.perform(post("/api/catalog/seller/products/{id}/actions/SUBMIT",id).header("X-Seller-Id",7)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
        mvc.perform(post("/api/catalog/internal/products/{id}/risk-decision",id).contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ON_SALE"));
        mvc.perform(get("/api/catalog/products/{id}",id)).andExpect(status().isOk());
        mvc.perform(post("/api/catalog/seller/products/{id}/actions/OFF_SHELF",id).header("X-Seller-Id",7)).andExpect(jsonPath("$.status").value("OFF_SHELF"));
    }

    @Test void lifecycleRuleRejectsIllegalTransition(){assertThatThrownBy(()->LifecyclePolicy.next("DRAFT","OFF_SHELF")).isInstanceOf(DomainException.class);}
}
