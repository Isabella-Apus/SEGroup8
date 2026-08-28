package com.segroup8.platform.controller;

import com.segroup8.platform.common.GlobalExceptionHandler;
import com.segroup8.platform.common.TransactionTradeTypeEnum;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.entity.Balance;
import com.segroup8.platform.entity.TransactionRecord;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.TransactionRecordMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.settlement.EscrowSettlementService;
import org.junit.jupiter.api.AfterEach;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("DOMAIN_E")
@Tag("UC23")
@ExtendWith(MockitoExtension.class)
class FinanceControllerUc23WebMvcTest {

    private MockMvc mockMvc;

    @Mock
    private EscrowSettlementService escrowSettlementService;

    @Mock
    private TransactionRecordMapper transactionRecordMapper;

    @Mock
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new FinanceController(escrowSettlementService, transactionRecordMapper, userMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .build();
        UserContext.setUserId(23L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void dashboardReturnsBothIsolatedBalancesForCurrentUser() throws Exception {
        when(escrowSettlementService.getOrInitBalance(23L))
                .thenReturn(balance("100.00", "40.00"));

        mockMvc.perform(get("/api/finance/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.personalBalance").value(100.0))
                .andExpect(jsonPath("$.data.businessBalance").value(40.0));

        verify(escrowSettlementService).getOrInitBalance(23L);
    }

    @Test
    void rechargeCreditsOnlyThePersonalAccount() throws Exception {
        when(escrowSettlementService.getOrInitBalance(23L))
                .thenReturn(balance("125.00", "40.00"));

        mockMvc.perform(post("/api/finance/recharge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":25.00,\"channel\":\"alipay\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.personalBalance").value(125.0))
                .andExpect(jsonPath("$.data.businessBalance").value(40.0));

        verify(escrowSettlementService).changePersonalBalance(
                23L,
                new BigDecimal("25.00"),
                null,
                "RECHARGE_ALIPAY",
                TransactionTradeTypeEnum.RECHARGE,
                "模拟充值入账");
        verify(escrowSettlementService, never()).changeBusinessBalance(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void invalidRechargeIsRejectedBeforeTheServiceWrites() throws Exception {
        mockMvc.perform(post("/api/finance/recharge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":0,\"channel\":\"WECHAT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(escrowSettlementService);
    }

    @Test
    void businessRecordsRequireOfficialSellerAndKeepTheirTradeMetadata() throws Exception {
        User buyer = new User();
        buyer.setId(23L);
        buyer.setRole("USER");
        when(userMapper.selectById(23L)).thenReturn(buyer);

        mockMvc.perform(get("/api/finance/business/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        User seller = new User();
        seller.setId(23L);
        seller.setRole("OFFICIAL_SELLER");
        when(userMapper.selectById(23L)).thenReturn(seller);
        TransactionRecord record = record(901L, "BUSINESS", "INCOME_BUSINESS", "99.00");
        when(transactionRecordMapper.selectList(any())).thenReturn(List.of(record));

        mockMvc.perform(get("/api/finance/business/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].orderId").value(901))
                .andExpect(jsonPath("$.data[0].accountType").value("BUSINESS"))
                .andExpect(jsonPath("$.data[0].tradeType").value("INCOME_BUSINESS"))
                .andExpect(jsonPath("$.data[0].amount").value(99.0));
    }

    private Balance balance(String personal, String business) {
        Balance balance = new Balance();
        balance.setUserId(23L);
        balance.setPersonalBalance(new BigDecimal(personal));
        balance.setBusinessBalance(new BigDecimal(business));
        return balance;
    }

    private TransactionRecord record(Long orderId, String accountType, String tradeType, String amount) {
        TransactionRecord record = new TransactionRecord();
        record.setId(1L);
        record.setOrderId(orderId);
        record.setUserId(23L);
        record.setAccountType(accountType);
        record.setTradeType(tradeType);
        record.setAmount(new BigDecimal(amount));
        record.setRemark("订单结算");
        record.setCreateTime(LocalDateTime.now());
        return record;
    }
}
