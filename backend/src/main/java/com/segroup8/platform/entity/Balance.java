package com.segroup8.platform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("balance")
public class Balance {

    @TableId
    private Long userId;
    private BigDecimal personalBalance;
    private BigDecimal businessBalance;
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getPersonalBalance() {
        return personalBalance;
    }

    public void setPersonalBalance(BigDecimal personalBalance) {
        this.personalBalance = personalBalance;
    }

    public BigDecimal getBusinessBalance() {
        return businessBalance;
    }

    public void setBusinessBalance(BigDecimal businessBalance) {
        this.businessBalance = businessBalance;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
