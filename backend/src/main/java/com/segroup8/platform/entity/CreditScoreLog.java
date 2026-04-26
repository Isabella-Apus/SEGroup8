package com.segroup8.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("credit_score_log")
public class CreditScoreLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 目标用户ID */
    private Long userId;

    /**
     * 变动时的身份角色：
     * BUYER  = 买家信用分变动
     * SELLER = 卖家信用分变动
     */
    private String role;

    /**
     * 分数变化值，正数为加分，负数为扣分
     * 例：+5、-10
     */
    private Integer delta;

    /**
     * 原因码枚举：
     *
     * 【加分类】
     * ORDER_COMPLETE        正常完成交易（买卖双方都加）
     * GOOD_REVIEW_RECEIVED  收到好评（卖家加分）
     * FIRST_TRADE           首次成功交易奖励
     *
     * 【扣分类】
     * REPORT_UPHELD         被举报成立
     * ORDER_DISPUTE         订单纠纷判定为己方责任
     * REFUND_ABUSE          恶意退款（买家）
     * CANCEL_OFTEN          频繁取消订单
     * BAD_REVIEW_RECEIVED   收到差评（卖家轻微扣分）
     *
     * 【恢复类】
     * ADMIN_ADJUST          管理员手动调整
     */
    private String reasonCode;

    /** 详细说明（可为空） */
    private String reasonDesc;

    /** 关联业务ID，如订单ID、举报ID */
    private Long refId;

    /** 触发该变动的操作者ID，系统自动触发则为null */
    private Long operatorId;

    private LocalDateTime createTime;

    // ---------- getters & setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getDelta() { return delta; }
    public void setDelta(Integer delta) { this.delta = delta; }

    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }

    public String getReasonDesc() { return reasonDesc; }
    public void setReasonDesc(String reasonDesc) { this.reasonDesc = reasonDesc; }

    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }

    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}