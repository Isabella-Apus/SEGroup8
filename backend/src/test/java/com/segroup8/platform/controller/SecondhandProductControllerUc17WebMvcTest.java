package com.segroup8.platform.controller;

import com.segroup8.platform.common.GlobalExceptionHandler;
import com.segroup8.platform.dto.SecondhandOrderCreateRequest;
import com.segroup8.platform.service.SearchBehaviorService;
import com.segroup8.platform.service.SecondhandProductService;
import com.segroup8.platform.vo.OrderVO;
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

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SecondhandProductControllerUc17WebMvcTest {

    private MockMvc mockMvc;

    @Mock
    private SecondhandProductService secondhandProductService;

    @Mock
    private SearchBehaviorService searchBehaviorService;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        SecondhandProductController controller =
                new SecondhandProductController(secondhandProductService, searchBehaviorService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .build();
    }

    @Test
    void buy_shouldAcceptAddressAndReturnPendingOrder() throws Exception {
        OrderVO order = new OrderVO();
        order.setId(170L);
        order.setOrderStatus(0);
        order.setTotalAmount(new BigDecimal("68.00"));

        when(secondhandProductService.buySecondhandProduct(eq(16L), any(SecondhandOrderCreateRequest.class)))
                .thenReturn(order);

        mockMvc.perform(post("/api/secondhand/16/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":1,\"remark\":\"当天可发货请备注\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(170))
                .andExpect(jsonPath("$.data.orderStatus").value(0));

        verify(secondhandProductService).buySecondhandProduct(eq(16L), any(SecondhandOrderCreateRequest.class));
    }
}
