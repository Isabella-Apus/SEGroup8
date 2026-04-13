package com.segroup8.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("browse_history")
public class BrowseHistory {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long productId;

    /**
     * NEW / SECONDHAND
     */
    private String productType;

    private LocalDateTime browseTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
