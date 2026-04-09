package com.segroup8.platform.controller;

import com.segroup8.platform.common.GlobalExceptionHandler;
import com.segroup8.platform.service.OrderService;
import com.segroup8.platform.vo.OrderVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerWebMvcTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        OrderController controller = new OrderController(orderService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        com.segroup8.platform.context.UserContext.clear();
    }

    @Test
    void refund_shouldAllowNullBodyAndReturnSuccess() throws Exception {
        OrderVO vo = new OrderVO();
        vo.setId(1L);

        when(orderService.refundMyOrder(eq(1L), eq(null))).thenReturn(vo);

        mockMvc.perform(post("/api/order/1/refund")
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(orderService).refundMyOrder(1L, null);
    }
}

