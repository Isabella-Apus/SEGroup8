package com.segroup8.platform.controller;

import com.segroup8.platform.common.GlobalExceptionHandler;
import com.segroup8.platform.dto.BargainApplyRequest;
import com.segroup8.platform.dto.BargainConfirmRequest;
import com.segroup8.platform.service.SecondhandTradeService;
import com.segroup8.platform.vo.ProductNegotiationVO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("DOMAIN_D")
@Tag("UC18")
@ExtendWith(MockitoExtension.class)
class SecondhandTradeControllerUc18WebMvcTest {

    private MockMvc mockMvc;

    @Mock
    private SecondhandTradeService secondhandTradeService;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new SecondhandTradeController(secondhandTradeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .build();
    }

    @Test
    void bargainApplyConfirmAndReject_shouldExposeStableRoutes() throws Exception {
        ProductNegotiationVO applied = new ProductNegotiationVO();
        applied.setId(18L);
        applied.setProductId(16L);
        applied.setStatus("APPLIED");

        ProductNegotiationVO accepted = new ProductNegotiationVO();
        accepted.setId(18L);
        accepted.setStatus("USED");
        accepted.setOrderId(180L);

        ProductNegotiationVO rejected = new ProductNegotiationVO();
        rejected.setId(19L);
        rejected.setStatus("REJECTED");

        when(secondhandTradeService.applyBargain(any(BargainApplyRequest.class))).thenReturn(applied);
        when(secondhandTradeService.confirmBargain(any(BargainConfirmRequest.class))).thenReturn(accepted);
        when(secondhandTradeService.rejectBargain(19L)).thenReturn(rejected);

        mockMvc.perform(post("/api/secondhand/trade/bargain/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":16,\"sellerUserId\":3,\"proposedPrice\":60.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPLIED"));

        mockMvc.perform(post("/api/secondhand/trade/bargain/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"negotiationId\":18,\"confirmedPrice\":60.00,\"createOrder\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("USED"))
                .andExpect(jsonPath("$.data.orderId").value(180));

        mockMvc.perform(post("/api/secondhand/trade/bargain/19/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        verify(secondhandTradeService).applyBargain(any(BargainApplyRequest.class));
        verify(secondhandTradeService).confirmBargain(any(BargainConfirmRequest.class));
        verify(secondhandTradeService).rejectBargain(19L);
    }
}
