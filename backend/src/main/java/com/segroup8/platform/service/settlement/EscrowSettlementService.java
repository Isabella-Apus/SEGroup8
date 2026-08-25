package com.segroup8.platform.service.settlement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.TransactionTradeTypeEnum;
import com.segroup8.platform.entity.Balance;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.OrderItem;
import com.segroup8.platform.entity.Product;
import com.segroup8.platform.entity.SecondhandProduct;
import com.segroup8.platform.entity.Shop;
import com.segroup8.platform.entity.TransactionRecord;
import com.segroup8.platform.entity.Voucher;
import com.segroup8.platform.mapper.BalanceMapper;
import com.segroup8.platform.mapper.ProductMapper;
import com.segroup8.platform.mapper.SecondhandProductMapper;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.mapper.TransactionRecordMapper;
import com.segroup8.platform.mapper.VoucherMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EscrowSettlementService {

    private final List<OrderSettlementStrategy> settlementStrategies;
    private final ProductMapper productMapper;
    private final ShopMapper shopMapper;
    private final SecondhandProductMapper secondhandProductMapper;
    private final BalanceMapper balanceMapper;
    private final TransactionRecordMapper transactionRecordMapper;
    private final VoucherMapper voucherMapper;

    public EscrowSettlementService(List<OrderSettlementStrategy> settlementStrategies,
            ProductMapper productMapper,
            ShopMapper shopMapper,
            SecondhandProductMapper secondhandProductMapper,
            BalanceMapper balanceMapper,
            TransactionRecordMapper transactionRecordMapper,
            VoucherMapper voucherMapper) {
        this.settlementStrategies = settlementStrategies;
        this.productMapper = productMapper;
        this.shopMapper = shopMapper;
        this.secondhandProductMapper = secondhandProductMapper;
        this.balanceMapper = balanceMapper;
        this.transactionRecordMapper = transactionRecordMapper;
        this.voucherMapper = voucherMapper;
    }

    public void releaseEscrow(OrderInfo order, List<OrderItem> items) {
        Map<String, BigDecimal> groupedAmount = new HashMap<>();
        Map<String, OrderSettlementStrategy> groupedStrategy = new HashMap<>();
        for (OrderItem item : items) {
            OrderSettlementStrategy strategy = resolveStrategy(item.getProductType());
            Long sellerUserId = resolveSellerUserId(item);
            if (sellerUserId == null) {
                throw new BusinessException(400, "无法定位订单卖家，结算失败");
            }
            BigDecimal itemAmount = item.getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity()));
            String key = sellerUserId + "#" + strategy.accountType().name();
            groupedAmount.merge(key, itemAmount, BigDecimal::add);
            groupedStrategy.putIfAbsent(key, strategy);
        }
        // Seller-issued vouchers reduce the amount credited to that seller.
        // Platform-issued vouchers leave the gross merchandise amount unchanged;
        // their discount is recorded in platformBearAmount and funded by platform.
        applySellerVoucherDiscount(order, groupedAmount);
        for (Map.Entry<String, BigDecimal> entry : groupedAmount.entrySet()) {
            String[] split = entry.getKey().split("#");
            Long sellerUserId = Long.valueOf(split[0]);
            SettlementAccountType accountType = SettlementAccountType.valueOf(split[1]);
            OrderSettlementStrategy strategy = groupedStrategy.get(entry.getKey());
            BigDecimal finalBalance = addBalanceWithOptimisticLock(sellerUserId, accountType, entry.getValue());

            TransactionRecord record = new TransactionRecord();
            record.setOrderId(order.getId());
            record.setUserId(sellerUserId);
            record.setAccountType(accountType.name());
            record.setChangeType(strategy.changeType());
            record.setTradeType(TransactionTradeTypeEnum.incomeByAccount(accountType).getCode());
            record.setAmount(entry.getValue());
            record.setBalanceAfter(finalBalance);
            record.setRemark("担保交易确认收货后释放资金");
            record.setCreateTime(LocalDateTime.now());
            transactionRecordMapper.insert(record);
        }
    }

    private void applySellerVoucherDiscount(OrderInfo order, Map<String, BigDecimal> groupedAmount) {
        if (order == null || order.getVoucherId() == null || order.getSellerBearAmount() == null
                || order.getSellerBearAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Voucher voucher = voucherMapper.selectById(order.getVoucherId());
        if (voucher == null || voucher.getShopId() == null) {
            return;
        }
        Shop shop = shopMapper.selectById(voucher.getShopId());
        if (shop == null || shop.getOwnerUserId() == null) {
            return;
        }
        String key = shop.getOwnerUserId() + "#" + SettlementAccountType.BUSINESS.name();
        BigDecimal gross = groupedAmount.get(key);
        if (gross != null) {
            groupedAmount.put(key, gross.subtract(order.getSellerBearAmount()).max(BigDecimal.ZERO));
        }
    }

    public BigDecimal changePersonalBalance(Long userId, BigDecimal amount, Long orderId, String changeType,
            TransactionTradeTypeEnum tradeType, String remark) {
        BigDecimal finalBalance = changeBalanceWithOptimisticLock(userId, SettlementAccountType.PERSONAL, amount);
        TransactionRecord record = new TransactionRecord();
        record.setOrderId(orderId);
        record.setUserId(userId);
        record.setAccountType(SettlementAccountType.PERSONAL.name());
        record.setChangeType(changeType);
        record.setTradeType((tradeType == null ? TransactionTradeTypeEnum.UNKNOWN : tradeType).getCode());
        record.setAmount(amount);
        record.setBalanceAfter(finalBalance);
        record.setRemark(remark);
        record.setCreateTime(LocalDateTime.now());
        transactionRecordMapper.insert(record);
        return finalBalance;
    }

    public BigDecimal changeBusinessBalance(Long userId, BigDecimal amount, Long orderId, String changeType,
            TransactionTradeTypeEnum tradeType, String remark) {
        BigDecimal finalBalance = changeBalanceWithOptimisticLock(userId, SettlementAccountType.BUSINESS, amount);
        TransactionRecord record = new TransactionRecord();
        record.setOrderId(orderId);
        record.setUserId(userId);
        record.setAccountType(SettlementAccountType.BUSINESS.name());
        record.setChangeType(changeType);
        record.setTradeType((tradeType == null ? TransactionTradeTypeEnum.UNKNOWN : tradeType).getCode());
        record.setAmount(amount);
        record.setBalanceAfter(finalBalance);
        record.setRemark(remark);
        record.setCreateTime(LocalDateTime.now());
        transactionRecordMapper.insert(record);
        return finalBalance;
    }

    private OrderSettlementStrategy resolveStrategy(String productType) {
        return settlementStrategies.stream()
                .filter(it -> it.supports(productType))
                .findFirst()
                .orElseThrow(() -> new BusinessException(400, "不支持的订单商品类型: " + productType));
    }

    private Long resolveSellerUserId(OrderItem item) {
        if (item == null || item.getProductType() == null || item.getProductId() == null) {
            return null;
        }
        if ("NEW".equalsIgnoreCase(item.getProductType())) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null) {
                return null;
            }
            Shop shop = shopMapper.selectById(product.getShopId());
            return shop == null ? null : shop.getOwnerUserId();
        }
        if ("SECONDHAND".equalsIgnoreCase(item.getProductType())) {
            SecondhandProduct secondhand = secondhandProductMapper.selectById(item.getProductId());
            return secondhand == null ? null : secondhand.getSellerUserId();
        }
        return null;
    }

    private BigDecimal addBalanceWithOptimisticLock(Long userId, SettlementAccountType accountType, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return changeBalanceWithOptimisticLock(userId, accountType, amount);
    }

    private BigDecimal changeBalanceWithOptimisticLock(Long userId, SettlementAccountType accountType,
            BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            Balance current = getOrInitBalance(userId);
            return accountType == SettlementAccountType.PERSONAL
                    ? (current.getPersonalBalance() == null ? BigDecimal.ZERO : current.getPersonalBalance())
                    : (current.getBusinessBalance() == null ? BigDecimal.ZERO : current.getBusinessBalance());
        }
        for (int i = 0; i < 3; i++) {
            Balance current = balanceMapper.selectById(userId);
            if (current == null) {
                Balance init = new Balance();
                init.setUserId(userId);
                init.setPersonalBalance(BigDecimal.ZERO);
                init.setBusinessBalance(BigDecimal.ZERO);
                init.setVersion(0);
                balanceMapper.insert(init);
                current = balanceMapper.selectById(userId);
            }
            if (current == null) {
                continue;
            }
            BigDecimal personal = current.getPersonalBalance() == null ? BigDecimal.ZERO : current.getPersonalBalance();
            BigDecimal business = current.getBusinessBalance() == null ? BigDecimal.ZERO : current.getBusinessBalance();
            BigDecimal targetPersonal = personal;
            BigDecimal targetBusiness = business;
            if (accountType == SettlementAccountType.PERSONAL) {
                targetPersonal = personal.add(amount);
            } else {
                targetBusiness = business.add(amount);
            }
            if (targetPersonal.compareTo(BigDecimal.ZERO) < 0 || targetBusiness.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(400, "账户余额不足");
            }
            int updated = balanceMapper.update(null, new UpdateWrapper<Balance>()
                    .set("personal_balance", targetPersonal)
                    .set("business_balance", targetBusiness)
                    .setSql("version = version + 1")
                    .eq("user_id", userId)
                    .eq("version", current.getVersion() == null ? 0 : current.getVersion()));
            if (updated > 0) {
                return accountType == SettlementAccountType.PERSONAL ? targetPersonal : targetBusiness;
            }
        }
        throw new BusinessException(409, "余额更新冲突，请重试");
    }

    public Balance getOrInitBalance(Long userId) {
        Balance balance = balanceMapper.selectOne(new LambdaQueryWrapper<Balance>()
                .eq(Balance::getUserId, userId)
                .last("limit 1"));
        if (balance != null) {
            return balance;
        }
        Balance init = new Balance();
        init.setUserId(userId);
        init.setPersonalBalance(BigDecimal.ZERO);
        init.setBusinessBalance(BigDecimal.ZERO);
        init.setVersion(0);
        balanceMapper.insert(init);
        return balanceMapper.selectById(userId);
    }
}
