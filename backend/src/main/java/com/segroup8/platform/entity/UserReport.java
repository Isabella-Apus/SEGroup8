package com.segroup8.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("user_report")
public class UserReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 举报人ID */
    private Long reporterId;

    /** 被举报人ID */
    private Long reportedId;

    /**
     * 举报人当时的身份
     * BUYER = 买家举报卖家
     * SELLER = 卖家举报买家
     */
    private String reporterRole;

    /**
     * 举报类型枚举值：
     * FRAUD          诈骗/虚假交易
     * FAKE_ITEM      商品与描述不符
     * BAD_ATTITUDE   态度恶劣/骚扰
     * REFUND_ABUSE   恶意退款
     * SPAM           刷单/广告骚扰
     * OTHER          其他
     */
    private String reasonType;

    /** 补充说明 */
    private String reasonDesc;

    /** 证据图片URL，逗号分隔 */
    private String evidenceUrls;

    /**
     * 审核状态：
     * 0 = 待审核
     * 1 = 成立（扣分）
     * 2 = 不成立（驳回）
     */
    private Integer status;

    /** 处理该举报的管理员ID */
    private Long adminId;

    /** 管理员审核备注 */
    private String adminRemark;

    private LocalDateTime auditTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ---------- getters & setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }

    public Long getReportedId() { return reportedId; }
    public void setReportedId(Long reportedId) { this.reportedId = reportedId; }

    public String getReporterRole() { return reporterRole; }
    public void setReporterRole(String reporterRole) { this.reporterRole = reporterRole; }

    public String getReasonType() { return reasonType; }
    public void setReasonType(String reasonType) { this.reasonType = reasonType; }

    public String getReasonDesc() { return reasonDesc; }
    public void setReasonDesc(String reasonDesc) { this.reasonDesc = reasonDesc; }

    public String getEvidenceUrls() { return evidenceUrls; }
    public void setEvidenceUrls(String evidenceUrls) { this.evidenceUrls = evidenceUrls; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }

    public String getAdminRemark() { return adminRemark; }
    public void setAdminRemark(String adminRemark) { this.adminRemark = adminRemark; }

    public LocalDateTime getAuditTime() { return auditTime; }
    public void setAuditTime(LocalDateTime auditTime) { this.auditTime = auditTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}