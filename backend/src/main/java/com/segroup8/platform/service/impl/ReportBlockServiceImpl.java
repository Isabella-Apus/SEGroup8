package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.segroup8.platform.common.AccessControl;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.dto.AdminReportAuditRequest;
import com.segroup8.platform.dto.UserBlockRequest;
import com.segroup8.platform.dto.UserReportRequest;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.entity.UserBlock;
import com.segroup8.platform.entity.UserReport;
import com.segroup8.platform.mapper.UserBlockMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.mapper.UserReportMapper;
import com.segroup8.platform.service.CreditService;
import com.segroup8.platform.service.ReportBlockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportBlockServiceImpl implements ReportBlockService {

    private final UserReportMapper userReportMapper;
    private final UserBlockMapper userBlockMapper;
    private final UserMapper userMapper;
    private final CreditService creditService;

    public ReportBlockServiceImpl(UserReportMapper userReportMapper,
                                   UserBlockMapper userBlockMapper,
                                   UserMapper userMapper,
                                   CreditService creditService) {
        this.userReportMapper = userReportMapper;
        this.userBlockMapper = userBlockMapper;
        this.userMapper = userMapper;
        this.creditService = creditService;
    }

    // ==================== 举报 ====================

    @Override
    @Transactional
    public void submitReport(UserReportRequest request) {
        Long reporterId = AccessControl.requireUserId();

        // 不能举报自己
        if (reporterId.equals(request.getReportedId())) {
            throw new BusinessException(400, "不能举报自己");
        }

        // 被举报人必须存在
        User reported = userMapper.selectById(request.getReportedId());
        if (reported == null) {
            throw new BusinessException(404, "被举报用户不存在");
        }

        // 防止重复举报：同一人对同一目标已有待审核(0)或成立(1)的举报
        int active = userReportMapper.countActiveReport(reporterId, request.getReportedId());
        if (active > 0) {
            throw new BusinessException(400, "您已对该用户提交过举报，请等待审核结果");
        }

        // 判断举报人身份：OFFICIAL_SELLER/SELLER 以卖家身份举报，其余以买家身份举报
        User reporter = userMapper.selectById(reporterId);
        String reporterRole = isSellerRole(reporter) ? "SELLER" : "BUYER";

        UserReport report = new UserReport();
        report.setReporterId(reporterId);
        report.setReportedId(request.getReportedId());
        report.setReporterRole(reporterRole);
        report.setReasonType(request.getReasonType());
        report.setReasonDesc(request.getReasonDesc());
        report.setEvidenceUrls(request.getEvidenceUrls());
        report.setStatus(0); // 待审核
        userReportMapper.insert(report);
    }

    @Override
    public IPage<UserReport> myReports(int page, int size) {
        Long reporterId = AccessControl.requireUserId();
        return userReportMapper.pageReports(
                new Page<>(page, size), null, null, reporterId);
    }

    @Override
    public IPage<UserReport> adminListReports(int page, int size,
                                               Integer status, Long reportedId) {
        return userReportMapper.pageReports(
                new Page<>(page, size), status, reportedId, null);
    }

    @Override
    @Transactional
    public void adminAuditReport(AdminReportAuditRequest request) {
        UserReport report = userReportMapper.selectById(request.getReportId());
        if (report == null) {
            throw new BusinessException(404, "举报记录不存在");
        }
        if (report.getStatus() != 0) {
            throw new BusinessException(400, "该举报已审核，不可重复处理");
        }
        if (request.getDecision() != 1 && request.getDecision() != 2) {
            throw new BusinessException(400, "decision 只能为 1（成立）或 2（驳回）");
        }

        Long adminId = AccessControl.requireUserId();

        // 更新举报状态
        report.setStatus(request.getDecision());
        report.setAdminId(adminId);
        report.setAdminRemark(request.getAdminRemark());
        report.setAuditTime(LocalDateTime.now());
        userReportMapper.updateById(report);

        // 举报成立 → 扣被举报人信用分
        if (request.getDecision() == 1) {
            // 被举报人的身份取反：举报人是BUYER说明被举报人是SELLER，反之亦然
            String reportedRole = "BUYER".equals(report.getReporterRole()) ? "SELLER" : "BUYER";
            creditService.onReportUpheld(
                    report.getId(),
                    report.getReportedId(),
                    reportedRole,
                    report.getReasonType(),
                    request.getCustomDelta()
            );
        }
    }

    // ==================== 拉黑 ====================

    @Override
    @Transactional
    public void blockUser(UserBlockRequest request) {
        Long blockerId = AccessControl.requireUserId();

        // 不能拉黑自己
        if (blockerId.equals(request.getTargetUserId())) {
            throw new BusinessException(400, "不能拉黑自己");
        }

        // 目标用户必须存在
        User target = userMapper.selectById(request.getTargetUserId());
        if (target == null) {
            throw new BusinessException(404, "目标用户不存在");
        }

        // 已拉黑则提示
        int exists = userBlockMapper.isBlocked(blockerId, request.getTargetUserId());
        if (exists > 0) {
            throw new BusinessException(400, "已拉黑该用户");
        }

        UserBlock block = new UserBlock();
        block.setBlockerId(blockerId);
        block.setBlockedId(request.getTargetUserId());
        userBlockMapper.insert(block);
    }

    @Override
    @Transactional
    public void unblockUser(UserBlockRequest request) {
        Long blockerId = AccessControl.requireUserId();
        int rows = userBlockMapper.unblock(blockerId, request.getTargetUserId());
        if (rows == 0) {
            throw new BusinessException(400, "您未拉黑该用户");
        }
    }

    @Override
    public List<UserBlock> myBlockList() {
        Long blockerId = AccessControl.requireUserId();
        return userBlockMapper.listMyBlocks(blockerId);
    }

    @Override
    public boolean isBlockedBy(Long targetUserId) {
        Long myId = AccessControl.requireUserId();
        return userBlockMapper.isBlocked(targetUserId, myId) > 0;
    }

    @Override
    public boolean isBlocking(Long targetUserId) {
        Long myId = AccessControl.requireUserId();
        return userBlockMapper.isBlocked(myId, targetUserId) > 0;
    }

    // ==================== 私有辅助 ====================

    private boolean isSellerRole(User user) {
        if (user == null || user.getRole() == null) return false;
        return "OFFICIAL_SELLER".equals(user.getRole())
                || "SELLER".equals(user.getRole());
    }
}