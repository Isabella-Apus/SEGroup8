import http from "./http";

// ==================== 信用分 ====================

/**
 * 获取我自己的信用信息（买家+卖家）
 */
export function getMyCreditApi() {
    return http.get("/credit/me");
}

/**
 * 获取指定用户的信用信息
 * @param {number} userId
 */
export function getUserCreditApi(userId) {
    return http.get(`/credit/${userId}`);
}

// ==================== 举报 ====================

/**
 * 提交举报
 * @param {object} payload
 * @param {number} payload.reportedId       被举报用户ID
 * @param {string} payload.reasonType       举报类型：FRAUD/FAKE_ITEM/BAD_ATTITUDE/REFUND_ABUSE/SPAM/OTHER
 * @param {string} [payload.reasonDesc]     补充说明（可选）
 * @param {string} [payload.evidenceUrls]   证据图片URL，逗号分隔（可选）
 */
export function submitReportApi(payload) {
    return http.post("/report-block/report", payload);
}

/**
 * 查询我提交的举报列表
 * @param {number} page
 * @param {number} size
 */
export function getMyReportsApi(page = 1, size = 10) {
    return http.get("/report-block/report/my", { params: { page, size } });
}

// ==================== 拉黑 ====================

/**
 * 拉黑某用户
 * @param {number} targetUserId
 */
export function blockUserApi(targetUserId) {
    return http.post("/report-block/block", { targetUserId });
}

/**
 * 取消拉黑
 * @param {number} targetUserId
 */
export function unblockUserApi(targetUserId) {
    return http.delete(`/report-block/block/${targetUserId}`);
}

/**
 * 查询我的拉黑列表
 */
export function getMyBlockListApi() {
    return http.get("/report-block/block/my");
}

/**
 * 查询我是否拉黑了某用户
 * @param {number} targetUserId
 */
export function isBlockingApi(targetUserId) {
    return http.get(`/report-block/block/check/${targetUserId}`);
}

/**
 * 查询某用户是否拉黑了我
 * @param {number} targetUserId
 */
export function isBlockedByApi(targetUserId) {
    return http.get(`/report-block/block/blocked-by/${targetUserId}`);
}

// ==================== 管理员 ====================

/**
 * 管理员查询举报列表
 * @param {number} page
 * @param {number} size
 * @param {number|null} status     0=待审核 1=成立 2=驳回 不传=全部
 * @param {number|null} reportedId 筛选被举报人
 */
export function adminListReportsApi(page = 1, size = 10, status = null, reportedId = null) {
    const params = { page, size };
    if (status !== null) params.status = status;
    if (reportedId !== null) params.reportedId = reportedId;
    return http.get("/admin/reports", { params });
}

/**
 * 管理员审核举报
 * @param {object} payload
 * @param {number} payload.reportId
 * @param {number} payload.decision      1=成立扣分  2=驳回
 * @param {string} [payload.adminRemark] 备注
 * @param {number} [payload.customDelta] 自定义扣分值（可选）
 */
export function adminAuditReportApi(payload) {
    return http.post("/admin/reports/audit", payload);
}

/**
 * 管理员手动调整信用分
 * @param {number} userId
 * @param {string} role    BUYER 或 SELLER
 * @param {number} delta   正数加分，负数扣分
 * @param {string} remark  原因说明
 */
export function adminCreditAdjustApi(userId, role, delta, remark) {
    return http.post("/admin/reports/credit-adjust", null, {
        params: { userId, role, delta, remark }
    });
}