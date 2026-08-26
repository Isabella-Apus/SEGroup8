package com.segroup8.platform.service.settlement;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.segroup8.platform.common.TransactionTradeTypeEnum;
import com.segroup8.platform.entity.Balance;
import com.segroup8.platform.entity.TransactionRecord;
import com.segroup8.platform.mapper.BalanceMapper;
import com.segroup8.platform.mapper.ProductMapper;
import com.segroup8.platform.mapper.SecondhandProductMapper;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.mapper.TransactionRecordMapper;
import com.segroup8.platform.mapper.VoucherMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UC23AccountIsolationTest {

    @Mock private ProductMapper productMapper;
    @Mock private ShopMapper shopMapper;
    @Mock private SecondhandProductMapper secondhandProductMapper;
    @Mock private BalanceMapper balanceMapper;
    @Mock private TransactionRecordMapper transactionRecordMapper;
    @Mock private VoucherMapper voucherMapper;

    private EscrowSettlementService service;

    @BeforeEach
    void setUp() {
        service = new EscrowSettlementService(
                List.of(), productMapper, shopMapper, secondhandProductMapper,
                balanceMapper, transactionRecordMapper, voucherMapper);
        Balance balance = new Balance();
        balance.setUserId(23L);
        balance.setPersonalBalance(new BigDecimal("100.00"));
        balance.setBusinessBalance(new BigDecimal("40.00"));
        balance.setVersion(3);
        when(balanceMapper.selectById(23L)).thenReturn(balance);
        when(balanceMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(transactionRecordMapper.insert(any(TransactionRecord.class))).thenReturn(1);
    }

    @Test
    void unitUc23001_personalAndBusinessAccountsRemainIsolated() {
        BigDecimal personalAfter = service.changePersonalBalance(
                23L, new BigDecimal("50.00"), null, "RECHARGE",
                TransactionTradeTypeEnum.RECHARGE, "充值");
        BigDecimal businessAfter = service.changeBusinessBalance(
                23L, new BigDecimal("50.00"), 9001L, "SETTLEMENT",
                TransactionTradeTypeEnum.INCOME_BUSINESS, "订单结算");

        assertEquals(new BigDecimal("150.00"), personalAfter);
        assertEquals(new BigDecimal("90.00"), businessAfter);

        ArgumentCaptor<TransactionRecord> records = ArgumentCaptor.forClass(TransactionRecord.class);
        org.mockito.Mockito.verify(transactionRecordMapper, org.mockito.Mockito.times(2)).insert(records.capture());
        assertEquals(SettlementAccountType.PERSONAL.name(), records.getAllValues().get(0).getAccountType());
        assertEquals(SettlementAccountType.BUSINESS.name(), records.getAllValues().get(1).getAccountType());

        ArgumentCaptor<Wrapper<Balance>> updates = ArgumentCaptor.forClass(Wrapper.class);
        org.mockito.Mockito.verify(balanceMapper, org.mockito.Mockito.times(2)).update(isNull(), updates.capture());
        String personalSql = ((UpdateWrapper<Balance>) updates.getAllValues().get(0)).getSqlSet();
        String businessSql = ((UpdateWrapper<Balance>) updates.getAllValues().get(1)).getSqlSet();
        assertTrue(personalSql.contains("personal_balance"));
        assertTrue(personalSql.contains("business_balance"));
        assertTrue(businessSql.contains("personal_balance"));
        assertTrue(businessSql.contains("business_balance"));
    }
}
