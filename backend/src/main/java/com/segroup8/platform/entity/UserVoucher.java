package com.segroup8.platform.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_voucher")
public class UserVoucher {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long voucherId;

    /**
     * 1=AVAILABLE, 2=USED, 3=EXPIRED
     */
    private Integer status;

    private LocalDateTime receivedTime;

    private Long usedOrderId;

    private LocalDateTime usedTime;

    private LocalDateTime expireTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
