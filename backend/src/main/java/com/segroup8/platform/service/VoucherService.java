package com.segroup8.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.VoucherSaveRequest;
import com.segroup8.platform.entity.Voucher;
import com.segroup8.platform.entity.UserVoucher;
import com.segroup8.platform.mapper.UserVoucherMapper;
import com.segroup8.platform.mapper.VoucherMapper;
import com.segroup8.platform.vo.VoucherVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_NOT_STARTED = 2;
    private static final int STATUS_ENDED = 3;
    private static final int STATUS_SOLD_OUT = 4;

    private static final int TYPE_AMOUNT = 1;
    private static final int TYPE_RATE = 2;

    private static final int ISSUER_TYPE_SELLER = 1;
    private static final int ISSUER_TYPE_ADMIN = 2;

    private static final int VOUCHER_TYPE_SELLER = 1;
    private static final int VOUCHER_TYPE_PLATFORM = 2;

    private static final int SCOPE_TYPE_SHOP = 1;
    private static final int SCOPE_TYPE_PLATFORM = 2;
    private static final int SCOPE_TYPE_PRODUCT = 3;

    private static final int USER_VOUCHER_STATUS_AVAILABLE = 1;
    private static final int USER_VOUCHER_STATUS_USED = 2;
    private static final int USER_VOUCHER_STATUS_EXPIRED = 3;

    private final VoucherMapper voucherMapper;
    private final UserVoucherMapper userVoucherMapper;

    public IPage<VoucherVO> listByShop(int page, int pageSize) {
        Long shopId = UserContext.getUserId();
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<Voucher>()
                .eq(Voucher::getShopId, shopId)
                .orderByDesc(Voucher::getCreateTime);
        IPage<Voucher> result = voucherMapper.selectPage(new Page<>(page, pageSize), wrapper);
        refreshStatuses(result.getRecords());
        return result.convert(this::toVO);
    }

    public IPage<VoucherVO> listForAdmin(int page, int pageSize, String name, Integer status, Integer scopeType) {
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<Voucher>()
                .like(StringUtils.hasText(name), Voucher::getName, name)
                .eq(status != null, Voucher::getStatus, status)
                .eq(scopeType != null, Voucher::getScopeType, scopeType)
                .orderByDesc(Voucher::getCreateTime);
        IPage<Voucher> result = voucherMapper.selectPage(new Page<>(page, pageSize), wrapper);
        refreshStatuses(result.getRecords());
        return result.convert(this::toVO);
    }

    public IPage<VoucherVO> pageAvailableForUser(int page, int pageSize) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<Voucher>()
                .ne(Voucher::getStatus, STATUS_DISABLED)
                .le(Voucher::getGrabStartTime, now)
                .ge(Voucher::getGrabEndTime, now)
                .apply("(total_count - received_count) > 0")
                .orderByAsc(Voucher::getGrabStartTime)
                .orderByAsc(Voucher::getGrabEndTime)
                .orderByDesc(Voucher::getCreateTime);
        IPage<Voucher> result = voucherMapper.selectPage(new Page<>(page, pageSize), wrapper);
        refreshStatuses(result.getRecords());
        return result.convert(this::toVO);
    }

    public IPage<VoucherVO> pageMine(int page, int pageSize) {
        Long userId = UserContext.getUserId();
        Page<UserVoucher> uvPage = userVoucherMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<UserVoucher>()
                        .eq(UserVoucher::getUserId, userId)
                        .orderByDesc(UserVoucher::getCreateTime));
        List<Long> voucherIds = uvPage.getRecords().stream().map(UserVoucher::getVoucherId).toList();

        Page<VoucherVO> result = new Page<>(page, pageSize);
        result.setTotal(uvPage.getTotal());
        if (voucherIds.isEmpty()) {
            result.setRecords(List.of());
            return result;
        }

        List<Voucher> vouchers = voucherMapper.selectBatchIds(voucherIds);
        Map<Long, Voucher> voucherMap = vouchers.stream().collect(Collectors.toMap(Voucher::getId, v -> v));
        LocalDateTime now = LocalDateTime.now();
        List<VoucherVO> records = uvPage.getRecords().stream()
                .map(uv -> {
                    Voucher voucher = voucherMap.get(uv.getVoucherId());
                    if (voucher == null) {
                        return null;
                    }
                    VoucherVO vo = toVO(voucher);
                    int myStatus = resolveMyStatus(uv, voucher, now);
                    vo.setMyStatus(myStatus);
                    vo.setMyStatusName(myStatusName(myStatus));
                    vo.setDaysToExpire(calcDaysToExpire(voucher.getEndTime(), now));
                    return vo;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(VoucherVO::getEndTime, Comparator.nullsLast(LocalDateTime::compareTo)))
                .toList();
        result.setRecords(records);
        return result;
    }

    public IPage<VoucherVO> pageMineAvailableForCheckout(int page, int pageSize, String shopIds, BigDecimal totalAmount) {
        Long userId = UserContext.getUserId();
        LocalDateTime now = LocalDateTime.now();
        Set<Long> checkoutShopIds = parseShopIds(shopIds);

        List<UserVoucher> uvRecords = userVoucherMapper.selectList(new LambdaQueryWrapper<UserVoucher>()
                .eq(UserVoucher::getUserId, userId)
                .eq(UserVoucher::getStatus, USER_VOUCHER_STATUS_AVAILABLE)
                .isNull(UserVoucher::getUsedOrderId)
                .orderByDesc(UserVoucher::getCreateTime));
        List<Long> voucherIds = uvRecords.stream().map(UserVoucher::getVoucherId).distinct().toList();

        Page<VoucherVO> result = new Page<>(page, pageSize);
        if (voucherIds.isEmpty()) {
            result.setTotal(0);
            result.setRecords(List.of());
            return result;
        }

        List<Voucher> vouchers = voucherMapper.selectBatchIds(voucherIds);
        List<VoucherVO> all = vouchers.stream()
                .filter(Objects::nonNull)
                .filter(v -> Integer.valueOf(STATUS_ACTIVE).equals(v.getStatus()))
                .filter(v -> v.getStartTime() == null || !now.isBefore(v.getStartTime()))
                .filter(v -> v.getEndTime() == null || !now.isAfter(v.getEndTime()))
                .filter(v -> matchShopScope(v, checkoutShopIds))
                .filter(v -> matchAmountThreshold(v, totalAmount))
                .map(v -> {
                    VoucherVO vo = toVO(v);
                    vo.setMyStatus(USER_VOUCHER_STATUS_AVAILABLE);
                    vo.setMyStatusName(myStatusName(USER_VOUCHER_STATUS_AVAILABLE));
                    return vo;
                })
                .sorted(checkoutVoucherComparator())
                .toList();

        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(all.size(), from + pageSize);
        result.setTotal(all.size());
        result.setRecords(from >= to ? List.of() : all.subList(from, to));
        return result;
    }

    public List<String> checkoutUnavailableReasons(String shopIds, BigDecimal totalAmount) {
        Long userId = UserContext.getUserId();
        LocalDateTime now = LocalDateTime.now();
        Set<Long> checkoutShopIds = parseShopIds(shopIds);

        List<UserVoucher> userVouchers = userVoucherMapper.selectList(new LambdaQueryWrapper<UserVoucher>()
                .eq(UserVoucher::getUserId, userId));
        if (userVouchers.isEmpty()) {
            return List.of("您还没有可用优惠券，可先去领券中心领取。");
        }

        boolean hasUsed = userVouchers.stream().anyMatch(uv -> Integer.valueOf(USER_VOUCHER_STATUS_USED).equals(uv.getStatus()));
        boolean hasExpired = userVouchers.stream().anyMatch(uv -> Integer.valueOf(USER_VOUCHER_STATUS_EXPIRED).equals(uv.getStatus())
                || (uv.getExpireTime() != null && now.isAfter(uv.getExpireTime())));
        boolean hasOccupied = userVouchers.stream().anyMatch(uv -> Integer.valueOf(USER_VOUCHER_STATUS_AVAILABLE).equals(uv.getStatus())
                && uv.getUsedOrderId() != null);

        List<Long> availableVoucherIds = userVouchers.stream()
                .filter(uv -> Integer.valueOf(USER_VOUCHER_STATUS_AVAILABLE).equals(uv.getStatus()))
                .filter(uv -> uv.getUsedOrderId() == null)
                .map(UserVoucher::getVoucherId)
                .distinct()
                .toList();

        boolean hasShopMismatch = false;
        boolean hasThresholdMismatch = false;
        if (!availableVoucherIds.isEmpty()) {
            List<Voucher> vouchers = voucherMapper.selectBatchIds(availableVoucherIds);
            hasShopMismatch = vouchers.stream().filter(Objects::nonNull)
                    .anyMatch(v -> !matchShopScope(v, checkoutShopIds));
            hasThresholdMismatch = vouchers.stream().filter(Objects::nonNull)
                    .anyMatch(v -> !matchAmountThreshold(v, totalAmount));
        }

        List<String> reasons = new java.util.ArrayList<>();
        if (hasThresholdMismatch) {
            reasons.add("门槛不足：当前结算金额未达到部分优惠券使用门槛。");
        }
        if (hasShopMismatch) {
            reasons.add("店铺不匹配：部分店铺券仅限指定店铺商品使用。");
        }
        if (hasOccupied) {
            reasons.add("已被占用：部分优惠券已绑定到其他未支付订单。");
        }
        if (hasUsed) {
            reasons.add("已使用：已使用过的优惠券不可再次使用。");
        }
        if (hasExpired) {
            reasons.add("已过期：过期优惠券不可使用。");
        }
        if (reasons.isEmpty()) {
            reasons.add("当前暂无可用优惠券，请稍后重试。");
        }
        return reasons;
    }

    public void claim(Long voucherId) {
        Long userId = UserContext.getUserId();
        Voucher voucher = getVoucherOrThrow(voucherId);
        validateVoucherClaimable(voucher);

        Long existed = userVoucherMapper.selectCount(new LambdaQueryWrapper<UserVoucher>()
                .eq(UserVoucher::getUserId, userId)
                .eq(UserVoucher::getVoucherId, voucherId));
        if (existed != null && existed > 0) {
            throw new BusinessException(400, "您已领取过该优惠券");
        }

        if (voucher.getTotalCount() != null && voucher.getReceivedCount() != null
                && voucher.getReceivedCount() >= voucher.getTotalCount()) {
            throw new BusinessException(400, "优惠券已领完");
        }

        LocalDateTime now = LocalDateTime.now();
        UserVoucher userVoucher = new UserVoucher();
        userVoucher.setUserId(userId);
        userVoucher.setVoucherId(voucherId);
        userVoucher.setStatus(USER_VOUCHER_STATUS_AVAILABLE);
        userVoucher.setReceivedTime(now);
        userVoucher.setExpireTime(voucher.getEndTime());
        userVoucher.setCreateTime(now);
        userVoucher.setUpdateTime(now);
        userVoucherMapper.insert(userVoucher);

        Voucher update = new Voucher();
        update.setId(voucherId);
        update.setReceivedCount((voucher.getReceivedCount() == null ? 0 : voucher.getReceivedCount()) + 1);
        voucherMapper.updateById(update);
    }

    public VoucherVO create(VoucherSaveRequest req) {
        validateRequest(req);
        Long shopId = UserContext.getUserId();

        Voucher voucher = new Voucher();
        voucher.setIssuerType(ISSUER_TYPE_SELLER);
        voucher.setVoucherType(VOUCHER_TYPE_SELLER);
        voucher.setIssuerUserId(shopId);
        voucher.setScopeType(SCOPE_TYPE_SHOP);
        voucher.setShopId(shopId);
        voucher.setProductId(null);
        voucher.setCanStack(Boolean.FALSE);
        fillFromRequest(voucher, req);
        voucher.setReceivedCount(0);
        voucher.setUsedCount(0);
        voucher.setStatus(calcStatus(req.getGrabStartTime(), req.getGrabEndTime(), 0, req.getTotalCount()));
        LocalDateTime now = LocalDateTime.now();
        voucher.setCreateTime(now);
        voucher.setUpdateTime(now);

        voucherMapper.insert(voucher);
        return toVO(voucherMapper.selectById(voucher.getId()));
    }

    public VoucherVO createForAdmin(VoucherSaveRequest req) {
        validateRequest(req);

        Voucher voucher = new Voucher();
        voucher.setIssuerType(ISSUER_TYPE_ADMIN);
        voucher.setVoucherType(VOUCHER_TYPE_PLATFORM);
        voucher.setIssuerUserId(UserContext.getUserId());
        voucher.setScopeType(SCOPE_TYPE_PLATFORM);
        voucher.setShopId(null);
        voucher.setProductId(null);
        voucher.setCanStack(Boolean.FALSE);
        fillFromRequest(voucher, req);
        voucher.setShopId(null);
        voucher.setProductId(null);
        voucher.setReceivedCount(0);
        voucher.setUsedCount(0);
        voucher.setStatus(calcStatus(req.getGrabStartTime(), req.getGrabEndTime(), 0, req.getTotalCount()));
        LocalDateTime now = LocalDateTime.now();
        voucher.setCreateTime(now);
        voucher.setUpdateTime(now);

        voucherMapper.insert(voucher);
        return toVO(voucherMapper.selectById(voucher.getId()));
    }

    public VoucherVO update(Long id, VoucherSaveRequest req) {
        validateRequest(req);
        Voucher voucher = getOwnedVoucher(id);

        fillFromRequest(voucher, req);
        int receivedCount = voucher.getReceivedCount() == null ? 0 : voucher.getReceivedCount();
        voucher.setStatus(calcStatus(req.getGrabStartTime(), req.getGrabEndTime(), receivedCount, req.getTotalCount()));

        voucherMapper.updateById(voucher);
        return toVO(voucherMapper.selectById(id));
    }

    public VoucherVO updateForAdmin(Long id, VoucherSaveRequest req) {
        validateRequest(req);
        Voucher voucher = getVoucherOrThrow(id);

        fillFromRequest(voucher, req);
        int receivedCount = voucher.getReceivedCount() == null ? 0 : voucher.getReceivedCount();
        voucher.setStatus(calcStatus(req.getGrabStartTime(), req.getGrabEndTime(), receivedCount, req.getTotalCount()));
        voucherMapper.updateById(voucher);
        return toVO(voucherMapper.selectById(id));
    }

    public void close(Long id) {
        Voucher voucher = getOwnedVoucher(id);
        voucher.setStatus(STATUS_DISABLED);
        voucherMapper.updateById(voucher);
    }

    public void closeForAdmin(Long id) {
        Voucher voucher = getVoucherOrThrow(id);
        voucher.setStatus(STATUS_DISABLED);
        voucherMapper.updateById(voucher);
    }

    public void delete(Long id) {
        Voucher voucher = getOwnedVoucher(id);
        if (voucher.getUsedCount() != null && voucher.getUsedCount() > 0) {
            throw new BusinessException(400, "已有用户使用，不能删除");
        }
        voucherMapper.deleteById(id);
    }

    public void deleteForAdmin(Long id) {
        Voucher voucher = getVoucherOrThrow(id);
        if (voucher.getUsedCount() != null && voucher.getUsedCount() > 0) {
            throw new BusinessException(400, "已有用户使用，不能删除");
        }
        if (voucher.getIssuerType() != null && voucher.getIssuerType() == ISSUER_TYPE_ADMIN
                && voucher.getCreateTime() == null) {
            // 兼容历史平台券：如果是旧数据且不是前端新建，允许清理
            voucherMapper.deleteById(id);
            return;
        }
        voucherMapper.deleteById(id);
    }

    private void validateRequest(VoucherSaveRequest req) {
        if (req.getType() == null || (req.getType() != TYPE_AMOUNT && req.getType() != TYPE_RATE)) {
            throw new BusinessException(400, "优惠券类型不合法");
        }

        boolean noThreshold = Boolean.TRUE.equals(req.getNoThreshold());
        BigDecimal threshold = req.getMinAmount();
        if (noThreshold) {
            threshold = BigDecimal.ZERO;
        } else {
            if (threshold == null || threshold.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "有门槛券必须设置大于0的门槛金额");
            }
        }

        if (req.getType() == TYPE_AMOUNT) {
            if (req.getDiscountAmount() == null || req.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "满减类型必须填写大于0的优惠金额");
            }
            if (!noThreshold && req.getDiscountAmount().compareTo(threshold) > 0) {
                throw new BusinessException(400, "优惠金额不能超过门槛金额");
            }
        }

        if (req.getType() == TYPE_RATE) {
            if (req.getDiscountRate() == null
                    || req.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0
                    || req.getDiscountRate().compareTo(BigDecimal.ONE) >= 0) {
                throw new BusinessException(400, "折扣类型必须填写0到1之间的折扣率");
            }
        }

        if (req.getGrabStartTime() == null || req.getGrabEndTime() == null) {
            throw new BusinessException(400, "抢券开始/结束时间不能为空");
        }
        if (req.getGrabEndTime().isBefore(req.getGrabStartTime())) {
            throw new BusinessException(400, "抢券结束时间不能早于抢券开始时间");
        }

        if (req.getStartTime() == null || req.getEndTime() == null) {
            throw new BusinessException(400, "使用开始/结束时间不能为空");
        }
        if (req.getEndTime().isBefore(req.getStartTime())) {
            throw new BusinessException(400, "使用结束时间不能早于使用开始时间");
        }
        if (req.getGrabEndTime().isAfter(req.getEndTime())) {
            throw new BusinessException(400, "领取结束时间不能晚于使用结束时间");
        }

        if (req.getTotalCount() == null || req.getTotalCount() <= 0) {
            throw new BusinessException(400, "发放总量至少为1");
        }
    }

    private void fillFromRequest(Voucher voucher, VoucherSaveRequest req) {
        voucher.setName(req.getName());
        voucher.setType(req.getType());
        voucher.setDiscountAmount(req.getDiscountAmount());
        voucher.setDiscountRate(req.getDiscountRate());
        voucher.setMinAmount(Boolean.TRUE.equals(req.getNoThreshold()) ? BigDecimal.ZERO : req.getMinAmount());
        voucher.setTotalCount(req.getTotalCount());
        // 当前业务：优惠券不允许叠加
        voucher.setCanStack(Boolean.FALSE);

        if (voucher.getIssuerType() != null && voucher.getIssuerType() == ISSUER_TYPE_ADMIN) {
            voucher.setVoucherType(VOUCHER_TYPE_PLATFORM);
            voucher.setScopeType(SCOPE_TYPE_PLATFORM);
            voucher.setShopId(null);
            voucher.setProductId(null);
        } else {
            voucher.setVoucherType(VOUCHER_TYPE_SELLER);
            // 卖家券固定为店铺范围，不允许商品级/全平台级
            voucher.setScopeType(SCOPE_TYPE_SHOP);
            voucher.setShopId(UserContext.getUserId());
            voucher.setProductId(null);
        }

        voucher.setGrabStartTime(req.getGrabStartTime());
        voucher.setGrabEndTime(req.getGrabEndTime());
        voucher.setStartTime(req.getStartTime());
        voucher.setEndTime(req.getEndTime());
    }

    private int calcStatus(LocalDateTime grabStartTime, LocalDateTime grabEndTime, Integer receivedCount, Integer totalCount) {
        if (totalCount != null && totalCount > 0) {
            int received = receivedCount == null ? 0 : receivedCount;
            if (received >= totalCount) {
                return STATUS_SOLD_OUT;
            }
        }
        LocalDateTime now = LocalDateTime.now();
        if (grabStartTime != null && now.isBefore(grabStartTime)) {
            return STATUS_NOT_STARTED;
        }
        if (grabEndTime != null && now.isAfter(grabEndTime)) {
            return STATUS_ENDED;
        }
        return STATUS_ACTIVE;
    }

    private Voucher getOwnedVoucher(Long id) {
        Long shopId = UserContext.getUserId();
        Voucher voucher = voucherMapper.selectById(id);
        if (voucher == null) {
            throw new BusinessException(404, "优惠券不存在");
        }
        if (voucher.getShopId() != null && !voucher.getShopId().equals(shopId)) {
            throw new BusinessException(403, "无权操作此优惠券");
        }
        return voucher;
    }

    private Voucher getVoucherOrThrow(Long id) {
        Voucher voucher = voucherMapper.selectById(id);
        if (voucher == null) {
            throw new BusinessException(404, "优惠券不存在");
        }
        return voucher;
    }

    private void refreshStatuses(List<Voucher> vouchers) {
        if (vouchers == null || vouchers.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Voucher voucher : vouchers) {
            if (voucher == null) {
                continue;
            }
            Integer oldStatus = voucher.getStatus();
            int newStatus = resolveCurrentStatus(voucher, now);
            if (!Objects.equals(oldStatus, newStatus)) {
                Voucher update = new Voucher();
                update.setId(voucher.getId());
                update.setStatus(newStatus);
                voucherMapper.updateById(update);
                voucher.setStatus(newStatus);
            }
        }
    }

    private int resolveCurrentStatus(Voucher voucher, LocalDateTime now) {
        if (voucher == null) {
            return STATUS_DISABLED;
        }
        if (Integer.valueOf(STATUS_DISABLED).equals(voucher.getStatus())) {
            return STATUS_DISABLED;
        }
        return calcStatus(voucher.getGrabStartTime(), voucher.getGrabEndTime(), voucher.getReceivedCount(), voucher.getTotalCount());
    }

    private void validateVoucherClaimable(Voucher voucher) {
        if (voucher == null) {
            throw new BusinessException(404, "优惠券不存在");
        }
        int currentStatus = resolveCurrentStatus(voucher, LocalDateTime.now());
        if (currentStatus == STATUS_DISABLED) {
            throw new BusinessException(400, "优惠券已关闭");
        }
        if (currentStatus == STATUS_NOT_STARTED) {
            throw new BusinessException(400, "优惠券未开始领取");
        }
        if (currentStatus == STATUS_ENDED) {
            throw new BusinessException(400, "优惠券已结束");
        }
        if (currentStatus == STATUS_SOLD_OUT) {
            throw new BusinessException(400, "优惠券已领完");
        }
        if (currentStatus != STATUS_ACTIVE) {
            throw new BusinessException(400, "优惠券当前不可领取");
        }
    }

    private VoucherVO toVO(Voucher v) {
        VoucherVO vo = new VoucherVO();
        vo.setId(v.getId());
        vo.setName(v.getName());
        vo.setIssuerType(v.getIssuerType());
        vo.setIssuerTypeName(issuerTypeName(v.getIssuerType()));
        vo.setVoucherType(v.getVoucherType());
        vo.setVoucherTypeName(voucherTypeName(v.getVoucherType()));
        vo.setScopeType(v.getScopeType());
        vo.setScopeTypeName(scopeTypeName(v.getScopeType()));
        vo.setShopId(v.getShopId());
        vo.setProductId(v.getProductId());
        vo.setCanStack(v.getCanStack());
        vo.setIssuerUserId(v.getIssuerUserId());
        vo.setType(v.getType());
        vo.setTypeName(v.getType() != null && v.getType() == TYPE_AMOUNT ? "满减" : "折扣");
        vo.setDiscountAmount(v.getDiscountAmount());
        vo.setDiscountRate(v.getDiscountRate());
        vo.setMinAmount(v.getMinAmount());
        vo.setTotalCount(v.getTotalCount());
        vo.setReceivedCount(v.getReceivedCount());
        vo.setUsedCount(v.getUsedCount());
        vo.setRemainCount(Math.max(0,
                (v.getTotalCount() == null ? 0 : v.getTotalCount()) - (v.getReceivedCount() == null ? 0 : v.getReceivedCount())));
        vo.setGrabStartTime(v.getGrabStartTime());
        vo.setGrabEndTime(v.getGrabEndTime());
        vo.setStartTime(v.getStartTime());
        vo.setEndTime(v.getEndTime());
        vo.setStatus(v.getStatus());
        vo.setStatusName(statusName(v.getStatus()));
        vo.setCreateTime(v.getCreateTime());
        return vo;
    }

    private String issuerTypeName(Integer issuerType) {
        if (issuerType == null) {
            return "未知";
        }
        switch (issuerType) {
            case ISSUER_TYPE_SELLER:
                return "商家";
            case ISSUER_TYPE_ADMIN:
                return "管理员";
            default:
                return "未知";
        }
    }

    private String voucherTypeName(Integer voucherType) {
        if (voucherType == null) {
            return "未知";
        }
        switch (voucherType) {
            case VOUCHER_TYPE_SELLER:
                return "卖家优惠券";
            case VOUCHER_TYPE_PLATFORM:
                return "平台优惠券";
            default:
                return "未知";
        }
    }

    private String scopeTypeName(Integer scopeType) {
        if (scopeType == null) {
            return "未知";
        }
        switch (scopeType) {
            case SCOPE_TYPE_SHOP:
                return "店铺";
            case SCOPE_TYPE_PLATFORM:
                return "全平台";
            case SCOPE_TYPE_PRODUCT:
                return "商品";
            default:
                return "未知";
        }
    }

    private int resolveMyStatus(UserVoucher uv, Voucher voucher, LocalDateTime now) {
        if (uv == null) {
            return USER_VOUCHER_STATUS_AVAILABLE;
        }
        if (Integer.valueOf(USER_VOUCHER_STATUS_USED).equals(uv.getStatus())) {
            return USER_VOUCHER_STATUS_USED;
        }
        LocalDateTime expireAt = uv.getExpireTime() != null ? uv.getExpireTime() : (voucher == null ? null : voucher.getEndTime());
        if (Integer.valueOf(USER_VOUCHER_STATUS_EXPIRED).equals(uv.getStatus())
                || (expireAt != null && now != null && now.isAfter(expireAt))) {
            return USER_VOUCHER_STATUS_EXPIRED;
        }
        return USER_VOUCHER_STATUS_AVAILABLE;
    }

    private String myStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case USER_VOUCHER_STATUS_AVAILABLE:
                return "未使用";
            case USER_VOUCHER_STATUS_USED:
                return "已使用";
            case USER_VOUCHER_STATUS_EXPIRED:
                return "已过期";
            default:
                return "未知";
        }
    }

    private long calcDaysToExpire(LocalDateTime endTime, LocalDateTime now) {
        if (endTime == null || now == null) {
            return 0L;
        }
        long seconds = Duration.between(now, endTime).getSeconds();
        if (seconds <= 0) {
            return 0L;
        }
        return (seconds + 86_399) / 86_400;
    }

    private Set<Long> parseShopIds(String shopIds) {
        if (!StringUtils.hasText(shopIds)) {
            return Set.of();
        }
        return java.util.Arrays.stream(shopIds.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(id -> {
                    try {
                        return Long.valueOf(id);
                    } catch (NumberFormatException ignore) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private boolean matchShopScope(Voucher voucher, Set<Long> checkoutShopIds) {
        if (voucher == null) {
            return false;
        }
        if (Integer.valueOf(SCOPE_TYPE_PLATFORM).equals(voucher.getScopeType())
                || Integer.valueOf(VOUCHER_TYPE_PLATFORM).equals(voucher.getVoucherType())) {
            return true;
        }
        if (checkoutShopIds == null || checkoutShopIds.isEmpty()) {
            // 结算店铺上下文缺失时，保守策略：仅允许平台券，店铺券不放行
            return false;
        }
        if (Integer.valueOf(SCOPE_TYPE_SHOP).equals(voucher.getScopeType())) {
            return voucher.getShopId() != null && checkoutShopIds.contains(voucher.getShopId());
        }
        return false;
    }

    private boolean matchAmountThreshold(Voucher voucher, BigDecimal totalAmount) {
        if (voucher == null) {
            return false;
        }
        if (totalAmount == null) {
            return true;
        }
        BigDecimal min = voucher.getMinAmount() == null ? BigDecimal.ZERO : voucher.getMinAmount();
        return totalAmount.compareTo(min) >= 0;
    }

    private Comparator<VoucherVO> checkoutVoucherComparator() {
        return Comparator
                .comparing((VoucherVO v) -> thresholdSortKey(v.getMinAmount()))
                .thenComparing((VoucherVO v) -> estimateDiscountValue(v), Comparator.reverseOrder())
                .thenComparing(VoucherVO::getId, Comparator.nullsLast(Long::compareTo));
    }

    private BigDecimal thresholdSortKey(BigDecimal minAmount) {
        return minAmount == null ? BigDecimal.ZERO : minAmount;
    }

    private BigDecimal estimateDiscountValue(VoucherVO voucher) {
        if (voucher == null || voucher.getType() == null) {
            return BigDecimal.ZERO;
        }
        if (voucher.getType() == TYPE_AMOUNT) {
            return voucher.getDiscountAmount() == null ? BigDecimal.ZERO : voucher.getDiscountAmount();
        }
        if (voucher.getType() == TYPE_RATE) {
            BigDecimal rate = voucher.getDiscountRate() == null ? BigDecimal.ZERO : voucher.getDiscountRate();
            return BigDecimal.ONE.subtract(rate);
        }
        return BigDecimal.ZERO;
    }

    private String statusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case STATUS_DISABLED:
                return "已关闭";
            case STATUS_ACTIVE:
                return "进行中";
            case STATUS_NOT_STARTED:
                return "未开始";
            case STATUS_ENDED:
                return "已结束";
            case STATUS_SOLD_OUT:
                return "已抢光";
            default:
                return "未知";
        }
    }
}
