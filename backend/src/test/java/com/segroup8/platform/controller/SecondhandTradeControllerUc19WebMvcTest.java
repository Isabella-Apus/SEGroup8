package com.segroup8.platform.controller;

import com.segroup8.platform.common.GlobalExceptionHandler;
import com.segroup8.platform.dto.AuctionBidRequest;
import com.segroup8.platform.dto.AuctionCreateRequest;
import com.segroup8.platform.service.SecondhandTradeService;
import com.segroup8.platform.vo.ProductAuctionVO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SecondhandTradeControllerUc19WebMvcTest {

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
    void auctionCreateQueryAndBid_shouldExposeSellerAndBuyerRoutes() throws Exception {
        ProductAuctionVO auction = new ProductAuctionVO();
        auction.setId(19L);
        auction.setProductId(16L);
        auction.setCurrentPrice(new BigDecimal("80.00"));
        auction.setStatus("ONGOING");

        ProductAuctionVO bid = new ProductAuctionVO();
        bid.setId(19L);
        bid.setProductId(16L);
        bid.setCurrentPrice(new BigDecimal("95.00"));
        bid.setStatus("ONGOING");

        when(secondhandTradeService.createAuction(any(AuctionCreateRequest.class))).thenReturn(auction);
        when(secondhandTradeService.getAuctionByProductId(16L)).thenReturn(auction);
        when(secondhandTradeService.placeBid(eq(19L), any(AuctionBidRequest.class))).thenReturn(bid);

        mockMvc.perform(post("/api/secondhand/trade/auction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":16,\"startPrice\":80.00,\"incrementAmount\":5.00,\"durationMinutes\":60}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ONGOING"));

        mockMvc.perform(get("/api/secondhand/trade/auction/product/16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentPrice").value(80.00));

        mockMvc.perform(post("/api/secondhand/trade/auction/19/bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bidAmount\":95.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentPrice").value(95.00));

        verify(secondhandTradeService).createAuction(any(AuctionCreateRequest.class));
        verify(secondhandTradeService).getAuctionByProductId(16L);
        verify(secondhandTradeService).placeBid(eq(19L), any(AuctionBidRequest.class));
    }
}
