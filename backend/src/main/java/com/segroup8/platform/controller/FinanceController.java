package com.segroup8.platform.controller;

import com.segroup8.platform.common.AccessControl;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.RoleEnum;
import com.segroup8.platform.common.Result;
import com.segroup8.platform.common.TransactionTradeTypeEnum;
import com.segroup8.platform.dto.FinanceRechargeRequest;
import com.segroup8.platform.entity.Balance;
import com.segroup8.platform.entity.TransactionRecord;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.TransactionRecordMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.settlement.EscrowSettlementService;
import com.segroup8.platform.vo.FinanceDashboardVO;
import com.segroup8.platform.vo.FinanceRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final EscrowSettlementService escrowSettlementService;
    private final TransactionRecordMapper transactionRecordMapper;
    private final UserMapper userMapper;

    public FinanceController(EscrowSettlementService escrowSettlementService,
            TransactionRecordMapper transactionRecordMapper,
            UserMapper userMapper) {
        this.escrowSettlementService = escrowSettlementService;
        this.transactionRecordMapper = transactionRecordMapper;
        this.userMapper = userMapper;
    }

    @Operation(summary = "获取我的财务看板")
    @GetMapping("/dashboard")
    public Result<FinanceDashboardVO> dashboard() {
        Long userId = AccessControl.requireUserId();
        Balance balance = escrowSettlementService.getOrInitBalance(userId);
        FinanceDashboardVO vo = new FinanceDashboardVO();
        vo.setPersonalBalance(balance.getPersonalBalance() == null ? BigDecimal.ZERO : balance.getPersonalBalance());
        vo.setBusinessBalance(balance.getBusinessBalance() == null ? BigDecimal.ZERO : balance.getBusinessBalance());
        return Result.success(vo);
    }

    @Operation(summary = "充值商城币（模拟）")
    @PostMapping("/recharge")
    public Result<FinanceDashboardVO> recharge(@Valid @RequestBody FinanceRechargeRequest request) {
        Long userId = AccessControl.requireUserId();
        String channel = request.getChannel() == null ? "WECHAT" : request.getChannel().trim().toUpperCase();
        escrowSettlementService.changePersonalBalance(
                userId,
                request.getAmount(),
                null,
                "RECHARGE_" + channel,
                TransactionTradeTypeEnum.RECHARGE,
                "模拟充值入账");
        Balance balance = escrowSettlementService.getOrInitBalance(userId);
        FinanceDashboardVO vo = new FinanceDashboardVO();
        vo.setPersonalBalance(balance.getPersonalBalance() == null ? BigDecimal.ZERO : balance.getPersonalBalance());
        vo.setBusinessBalance(balance.getBusinessBalance() == null ? BigDecimal.ZERO : balance.getBusinessBalance());
        return Result.success(vo);
    }

    @Operation(summary = "个人钱包流水")
    @GetMapping("/my-wallet/records")
    public Result<List<FinanceRecordVO>> walletRecords() {
        Long userId = AccessControl.requireUserId();
        List<TransactionRecord> records = transactionRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TransactionRecord>()
                        .eq(TransactionRecord::getUserId, userId)
                        .in(TransactionRecord::getAccountType, "PERSONAL", "1")
                        .orderByDesc(TransactionRecord::getCreateTime)
                        .last("limit 100"));
        return Result.success(records.stream().map(this::toVO).toList());
    }

    @Operation(summary = "商家经营流水")
    @GetMapping("/business/records")
    public Result<List<FinanceRecordVO>> businessRecords() {
        Long userId = AccessControl.requireUserId();
        User user = userMapper.selectById(userId);
        if (user == null || !RoleEnum.OFFICIAL_SELLER.name().equals(user.getRole())) {
            throw new BusinessException(403, "仅官方卖家可查看经营流水");
        }
        List<TransactionRecord> records = transactionRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TransactionRecord>()
                        .eq(TransactionRecord::getUserId, userId)
                        .in(TransactionRecord::getAccountType, "BUSINESS", "2")
                        .orderByDesc(TransactionRecord::getCreateTime)
                        .last("limit 100"));
        return Result.success(records.stream().map(this::toVO).toList());
    }

    private FinanceRecordVO toVO(TransactionRecord record) {
        FinanceRecordVO vo = new FinanceRecordVO();
        vo.setId(record.getId());
        vo.setOrderId(record.getOrderId());
        vo.setAccountType(record.getAccountType());
        vo.setTradeType(record.getTradeType());
        vo.setTradeTypeName(TransactionTradeTypeEnum.of(record.getTradeType()).getDesc());
        vo.setAmount(record.getAmount());
        vo.setRemark(record.getRemark());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }
}
