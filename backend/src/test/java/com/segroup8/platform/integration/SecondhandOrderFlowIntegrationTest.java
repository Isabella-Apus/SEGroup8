package com.segroup8.platform.integration;

import com.segroup8.platform.utils.JwtUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("DOMAIN_D")
@Tag("UC20")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecondhandOrderFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    @Sql(statements = {
            "INSERT INTO `order_info` (`id`, `order_no`, `buyer_user_id`, `total_amount`, `pay_status`, `order_status`, `refund_status`, `receiver_name`, `receiver_phone`, `receiver_province`, `receiver_city`, `receiver_detail_address`, `version`, `create_time`, `update_time`) VALUES (151, 'ORD_SECONDHAND_151', 1, 80.00, 1, 1, 0, '买家1', '13800000001', '广东省', '广州市', '天河路1号', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO `order_item` (`order_id`, `product_type`, `product_id`, `product_name`, `price`, `quantity`) VALUES (151, 'SECONDHAND', 5, 'test secondhand product', 80.00, 1)",
            "INSERT INTO `order_info` (`id`, `order_no`, `buyer_user_id`, `total_amount`, `pay_status`, `order_status`, `refund_status`, `receiver_name`, `receiver_phone`, `receiver_province`, `receiver_city`, `receiver_detail_address`, `version`, `create_time`, `update_time`) VALUES (152, 'ORD_SECONDHAND_152', 1, 80.00, 1, 1, 0, '买家1', '13800000001', '广东省', '广州市', '天河路1号', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO `order_item` (`order_id`, `product_type`, `product_id`, `product_name`, `price`, `quantity`) VALUES (152, 'SECONDHAND', 5, 'test secondhand product', 80.00, 1)",
            "INSERT INTO `logistics_path_template` (`id`, `origin_region`, `dest_region`, `path_nodes`) VALUES (900, '华北', '华南', '[\"天津分拨中心\",\"广东省分拨中心\"]')"
    })
    void secondhandSellerCanViewShipAndPushLogistics() throws Exception {
        String buyerToken = jwtUtils.createToken(1L, "buyer1", "USER");
        String sellerToken = jwtUtils.createToken(3L, "seller1", "OFFICIAL_SELLER");

        mockMvc.perform(get("/api/order/detail/151")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(151))
                .andExpect(jsonPath("$.data.items[0].productType").value("SECONDHAND"));

        mockMvc.perform(get("/api/order/detail/151")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(151))
                .andExpect(jsonPath("$.data.items[0].sellerUserId").value(3));

        mockMvc.perform(post("/api/order/151/ship")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "originProvince": "云南省",
                                  "originCity": "昆明市",
                                  "originDetail": "五华区1号"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").value(2))
                .andExpect(jsonPath("$.data.logisticsStatus").value("IN_TRANSIT"));

        mockMvc.perform(post("/api/logistics/push-next")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType("application/json")
                        .content("{\"orderId\":151}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderId").value(151))
                .andExpect(jsonPath("$.data.nodeName").exists());

        mockMvc.perform(get("/api/logistics/order/151/trace")
                .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].nodeName").value("云南省分拨中心"));

        mockMvc.perform(get("/api/logistics/order/151/trace")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(post("/api/order/152/ship")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "originProvince": "四川省",
                                  "originCity": "成都市",
                                  "originDetail": "四川省成都市高新区1号"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").value(2));

        mockMvc.perform(post("/api/order/152/ship")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").value(2));

        mockMvc.perform(get("/api/logistics/order/152/trace")
                .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].nodeName").value("四川省分拨中心"));
    }
}
