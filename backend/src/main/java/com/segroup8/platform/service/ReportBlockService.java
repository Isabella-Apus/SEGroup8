package com.segroup8.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.segroup8.platform.dto.AdminReportAuditRequest;
import com.segroup8.platform.dto.UserBlockRequest;
import com.segroup8.platform.dto.UserReportRequest;
import com.segroup8.platform.entity.UserBlock;
import com.segroup8.platform.entity.UserReport;

import java.util.List;

public interface ReportBlockService {

    /**
     * 提交举报
     * 规则：
     * 1. 不能举报自己
     * 2. 同一人对同一目标已有待审核或成立的举报时，拒绝重复提交
     * 3. 举报人身份由后端根据登录用户role自动判断
     *    - USER / OFFICIAL_SELLER 都可以互相举报
     */
    void submitReport(UserReportRequest request);

    /**
     * 查询我提交的举报列表（分页）
     */
    IPage<UserReport> myReports(int page, int size);

    /**
     * 管理员分页查询举报列表
     * @param status    0=待审核 1=成立 2=驳回 null=全部
     * @param reportedId 筛选被举报人，null=不筛选
     */
    IPage<UserReport> adminListReports(int page, int size,
                                       Integer status, Long reportedId);

    /**
     * 管理员审核举报
     * - decision=1：举报成立，调用 CreditService.onReportUpheld 扣分
     * - decision=2：举报不成立，仅更新状态
     */
    void adminAuditReport(AdminReportAuditRequest request);

    /**
     * 拉黑某用户
     * 规则：
     * 1. 不能拉黑自己
     * 2. 已拉黑则提示重复
     * 3. 买家可以拉黑卖家，卖家也可以拉黑买家
     */
    void blockUser(UserBlockRequest request);

    /**
     * 取消拉黑
     */
    void unblockUser(UserBlockRequest request);

    /**
     * 查询我的拉黑列表
     */
    List<UserBlock> myBlockList();

    /**
     * 判断当前登录用户是否被某用户拉黑
     */
    boolean isBlockedBy(Long targetUserId);

    /**
     * 判断当前登录用户是否拉黑了某用户
     */
    boolean isBlocking(Long targetUserId);
}