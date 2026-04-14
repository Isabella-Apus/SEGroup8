package com.segroup8.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.VoucherSaveRequest;
import com.segroup8.platform.entity.Voucher;
import com.segroup8.platform.mapper.VoucherMapper;
import com.segroup8.platform.vo.VoucherVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherMapper voucherMapper;

    public IPage<VoucherVO> listByShop(int page, int pageSize) {
        Long shopId = UserContext.getUserId();
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<Voucher>()
                .eq(Voucher::getShopId, shopId)
                .orderByDesc(Voucher::getCreateTime);
        IPage<Voucher> result = voucherMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return result.convert(this::toVO);
    }

    public VoucherVO create(VoucherSaveRequest req) {
        validateRequest(req);
        Long shopId = UserContext.getUserId();
        Voucher voucher = new Voucher();
        voucher.setShopId(shopId);
        fillFromRequest(voucher, req);
        voucher.setUsedCount(0);
        voucher.setStatus(calcStatus(req.getStartTime(), req.getEndTime()));
        voucherMapper.insert(voucher);
        return toVO(voucherMapper.selectById(voucher.getId()));
    }

    public VoucherVO update(Long id, VoucherSaveRequest req) {
        validateRequest(req);
        Voucher voucher = getOwnedVoucher(id);
        fillFromRequest(voucher, req);
        voucher.setStatus(calcStatus(req.getStartTime(), req.getEndTime()));
        voucherMapper.updateById(voucher);
        return toVO(voucherMapper.selectById(id));
    }

    public void close(Long id) {
        Voucher voucher = getOwnedVoucher(id);
        voucher.setStatus(0);
        voucherMapper.updateById(voucher);
    }

    public void delete(Long id) {
        Voucher voucher = getOwnedVoucher(id);
        if (voucher.getUsedCount() > 0) {
            throw new BusinessException(400, "已有用户使用，不能删除");
        }
        voucherMapper.deleteById(id);
    }

    private void validateRequest(VoucherSaveRequest req) {
        if (req.getType() == 1 && req.getDiscountAmount() == null) {
            throw new BusinessException(400, "满减类型必须填写优惠金额");
        }
        if (req.getType() == 2 && req.getDiscountRate() == null) {
            throw new BusinessException(400, "折扣类型必须填写折扣率");
        }
        if (req.getEndTime().isBefore(req.getStartTime())) {
            throw new BusinessException(400, "结束时间不能早于开始时间");
        }
    }

    private void fillFromRequest(Voucher voucher, VoucherSaveRequest req) {
        voucher.setName(req.getName());
        voucher.setType(req.getType());
        voucher.setDiscountAmount(req.getDiscountAmount());
        voucher.setDiscountRate(req.getDiscountRate());
        voucher.setMinAmount(req.getMinAmount());
        voucher.setTotalCount(req.getTotalCount());
        voucher.setStartTime(req.getStartTime());
        voucher.setEndTime(req.getEndTime());
    }

    private int calcStatus(LocalDateTime start, LocalDateTime end) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(start)) return 2;
        if (now.isAfter(end)) return 3;
        return 1;
    }

    private Voucher getOwnedVoucher(Long id) {
        Long shopId = UserContext.getUserId();
        Voucher voucher = voucherMapper.selectById(id);
        if (voucher == null) {
            throw new BusinessException(404, "优惠券不存在");
        }
        if (!voucher.getShopId().equals(shopId)) {
            throw new BusinessException(403, "无权操作此优惠券");
        }
        return voucher;
    }

    private VoucherVO toVO(Voucher v) {
        VoucherVO vo = new VoucherVO();
        vo.setId(v.getId());
        vo.setName(v.getName());
        vo.setType(v.getType());
        vo.setTypeName(v.getType() == 1 ? "满减" : "折扣");
        vo.setDiscountAmount(v.getDiscountAmount());
        vo.setDiscountRate(v.getDiscountRate());
        vo.setMinAmount(v.getMinAmount());
        vo.setTotalCount(v.getTotalCount());
        vo.setUsedCount(v.getUsedCount());
        vo.setRemainCount(v.getTotalCount() - v.getUsedCount());
        vo.setStartTime(v.getStartTime());
        vo.setEndTime(v.getEndTime());
        vo.setStatus(v.getStatus());
        String statusName;
        switch (v.getStatus()) {
            case 0: statusName = "已关闭"; break;
            case 1: statusName = "进行中"; break;
            case 2: statusName = "未开始"; break;
            case 3: statusName = "已结束"; break;
            default: statusName = "未知"; break;
        }
        vo.setStatusName(statusName);
        vo.setCreateTime(v.getCreateTime());
        return vo;
    }
}