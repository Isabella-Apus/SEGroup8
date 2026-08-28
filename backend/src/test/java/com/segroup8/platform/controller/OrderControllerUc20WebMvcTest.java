package com.segroup8.platform.controller;

import com.segroup8.platform.common.GlobalExceptionHandler;
import com.segroup8.platform.dto.OrderShipRequest;
import com.segroup8.platform.service.OrderService;
import com.segroup8.platform.vo.OrderVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("DOMAIN_D")
@Tag("UC20")
@ExtendWith(MockitoExtension.class)
class OrderControllerUc20WebMvcTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .build();
    }

    @Test
    void shipAndConfirmReceive_shouldExposeStableRoutes() throws Exception {
        OrderVO shipped = new OrderVO();
        shipped.setId(200L);
        shipped.setOrderStatus(2);
        shipped.setLogisticsStatus("IN_TRANSIT");

        OrderVO received = new OrderVO();
        received.setId(200L);
        received.setOrderStatus(3);

        when(orderService.shipSellerOrder(eq(200L), any(OrderShipRequest.class))).thenReturn(shipped);
        when(orderService.confirmReceiveMyOrder(200L)).thenReturn(received);

        mockMvc.perform(post("/api/order/200/ship")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originProvince\":\"广东省\",\"originCity\":\"广州市\",\"originDetail\":\"天河区1号\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value(2))
                .andExpect(jsonPath("$.data.logisticsStatus").value("IN_TRANSIT"));

        mockMvc.perform(post("/api/order/200/confirm-receive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value(3));

        verify(orderService).shipSellerOrder(eq(200L), any(OrderShipRequest.class));
        verify(orderService).confirmReceiveMyOrder(200L);
    }
}
