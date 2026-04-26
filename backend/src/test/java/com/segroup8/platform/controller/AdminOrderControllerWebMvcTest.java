package com.segroup8.platform.controller;

import com.segroup8.platform.common.GlobalExceptionHandler;
import com.segroup8.platform.common.RoleEnum;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.entity.OrderAfterSaleLog;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.OrderAfterSaleLogMapper;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.OrderItemMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.realtime.RealtimePushService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerWebMvcTest {

    private MockMvc mockMvc;

    @Mock
    private OrderInfoMapper orderInfoMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private OrderAfterSaleLogMapper orderAfterSaleLogMapper;
    @Mock
    private RealtimePushService realtimePushService;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        AdminOrderController controller = new AdminOrderController(
                orderInfoMapper,
                orderItemMapper,
                userMapper,
                orderAfterSaleLogMapper,
                realtimePushService, null);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void approveRefund_shouldFailWhenRemarkTooLong() throws Exception {
        Long adminId = 1L;
        UserContext.setUserId(adminId);

        String tooLong = "a".repeat(256);

        mockMvc.perform(post("/api/admin/orders/1/refund/approve")
                .contentType("application/json")
                .content("{\"remark\":\"" + tooLong + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("审核意见最多255字"));

        verify(orderInfoMapper, never()).selectById(any());
    }

    @Test
    void afterSaleLogs_shouldReturnSuccessList() throws Exception {
        Long adminId = 1L;
        UserContext.setUserId(adminId);

        User admin = new User();
        admin.setId(adminId);
        admin.setRole(RoleEnum.ADMIN.name());
        when(userMapper.selectById(adminId)).thenReturn(admin);

        OrderAfterSaleLog log = new OrderAfterSaleLog();
        log.setId(10L);
        log.setOrderId(99L);
        log.setAction("APPLY");
        log.setOperatorRole("BUYER");
        log.setOperatorUserId(100L);
        log.setRemark("买家申请退货");
        log.setCreateTime(LocalDateTime.now());

        when(orderAfterSaleLogMapper.selectList(any())).thenReturn(List.of(log));

        mockMvc.perform(get("/api/admin/orders/99/after-sale-logs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].orderId").value(99))
                .andExpect(jsonPath("$.data[0].action").value("APPLY"))
                .andExpect(jsonPath("$.data[0].operatorRole").value("BUYER"));

        verify(orderAfterSaleLogMapper).selectList(any());
        verify(userMapper).selectById(eq(adminId));
    }
}
