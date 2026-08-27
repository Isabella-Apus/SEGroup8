package com.segroup8.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("DOMAIN_B")
@Tag("UC06")
class CatalogApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;

    @BeforeEach
    void seed() {
        db.update("delete from products");
        db.update("insert into products(id,seller_id,shop_id,name,description,category,price,stock,status,updated_at) "
                + "values(1,7,8,'Java 图书','微服务实践','BOOK',59.00,10,'ON_SALE',TIMESTAMP '2026-01-02 00:00:00')");
        db.update("insert into products(id,seller_id,shop_id,name,description,category,price,stock,status,updated_at) "
                + "values(2,7,8,'Java 入门','基础教程','BOOK',39.00,5,'ON_SALE',TIMESTAMP '2026-01-01 00:00:00')");
        db.update("insert into products(id,seller_id,shop_id,name,description,category,price,stock,status,updated_at) "
                + "values(3,9,10,'Java 下架书','不可见','BOOK',29.00,2,'OFF_SHELF',CURRENT_TIMESTAMP)");
    }

    @Test
    void combinedSearchAndPublicDetailUseTheDatabase() throws Exception {
        mvc.perform(get("/api/catalog/products").param("keyword", "Java").param("category", "BOOK")
                        .param("minPrice", "50").param("maxPrice", "60"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("ON_SALE"));
        mvc.perform(get("/api/catalog/products/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Java 图书"));
    }

    @Test
    void filtersSortsEmptyAndExceptionPaths() throws Exception {
        mvc.perform(get("/api/catalog/products").param("shopId", "8").param("category", "BOOK")
                        .param("sort", "priceAsc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[1].id").value(1));
        mvc.perform(get("/api/catalog/products").param("sort", "priceDesc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
        mvc.perform(get("/api/catalog/products").param("keyword", "不存在"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/catalog/products/3")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
        mvc.perform(get("/api/catalog/products").param("minPrice", "非法数字"))
                .andExpect(status().isBadRequest());
    }
}
