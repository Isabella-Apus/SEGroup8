package com.segroup8.platform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.platform.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class DomainEIntegrationTestBase {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate db;

    @Autowired
    protected JwtUtils jwtUtils;

    protected String buyerToken;
    protected String adminToken;
    protected String sellerToken;

    @BeforeEach
    void resetDomainEData() {
        db.update("delete from chat_message");
        db.update("delete from chat_conversation");
        db.update("delete from user_voucher");
        db.update("delete from voucher");
        db.update("delete from user_block");
        db.update("delete from notification");
        db.update("delete from transaction_record");
        db.update("delete from balance");
        db.update("delete from address");
        db.update("insert into balance(user_id, personal_balance, business_balance, version) "
                + "values(1, 100.00, 0.00, 0)");
        db.update("insert into balance(user_id, personal_balance, business_balance, version) "
                + "values(3, 0.00, 0.00, 0)");
        db.update("insert into address(user_id, receiver_name, receiver_phone, province, city, "
                + "detail_address, is_default) values(1, '测试买家', '13800000000', "
                + "'北京市', '北京市', '测试路1号', 1)");

        buyerToken = jwtUtils.createToken(1L, "buyer1", "USER");
        adminToken = jwtUtils.createToken(2L, "admin1", "ADMIN");
        sellerToken = jwtUtils.createToken(3L, "seller1", "OFFICIAL_SELLER");
    }

    protected long createVoucher(String path, String token, String name, String discountAmount)
            throws Exception {
        MvcResult result = mvc.perform(post(path)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(voucherBody(name, discountAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return responseData(result).path("id").asLong();
    }

    protected String voucherBody(String name, String discountAmount) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("type", 1);
        body.put("discountAmount", new BigDecimal(discountAmount));
        body.put("minAmount", new BigDecimal("100.00"));
        body.put("noThreshold", false);
        body.put("totalCount", 50);
        body.put("grabStartTime", now.minusDays(1));
        body.put("grabEndTime", now.plusDays(2));
        body.put("startTime", now.minusDays(1));
        body.put("endTime", now.plusDays(7));
        return objectMapper.writeValueAsString(body);
    }

    protected JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
