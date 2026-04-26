package com.segroup8.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.segroup8.platform.entity.CreditScoreLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CreditScoreLogMapper extends BaseMapper<CreditScoreLog> {

    /**
     * 分页查询某用户的信用分变动记录
     * role = null 时查全部（买家+卖家）
     */
    @Select("<script>" +
            "SELECT * FROM credit_score_log" +
            "<where>" +
            "  AND user_id = #{userId}" +
            "  <if test='role != null'> AND role = #{role} </if>" +
            "</where>" +
            " ORDER BY create_time DESC" +
            "</script>")
    IPage<CreditScoreLog> pageByUser(
            Page<CreditScoreLog> page,
            @Param("userId") Long userId,
            @Param("role") String role
    );

    /**
     * 查询某用户某身份最近N条变动记录
     */
    @Select("<script>" +
            "SELECT * FROM credit_score_log" +
            " WHERE user_id = #{userId}" +
            "<if test='role != null'> AND role = #{role} </if>" +
            " ORDER BY create_time DESC" +
            " LIMIT #{limit}" +
            "</script>")
    java.util.List<CreditScoreLog> recentLogs(
            @Param("userId") Long userId,
            @Param("role") String role,
            @Param("limit") int limit
    );

    /**
     * 统计某用户某身份近N天的总变动量
     * 用于判断短期内是否异常波动
     */
    @Select("SELECT COALESCE(SUM(delta), 0) FROM credit_score_log " +
            "WHERE user_id = #{userId} " +
            "  AND role = #{role} " +
            "  AND create_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int sumDeltaInDays(
            @Param("userId") Long userId,
            @Param("role") String role,
            @Param("days") int days
    );

    /**
     * 统计某用户某原因码的历史触发次数
     * 用于判断是否达到上限（如每天最多加一次完成订单分）
     */
    @Select("SELECT COUNT(*) FROM credit_score_log " +
            "WHERE user_id = #{userId} " +
            "  AND role = #{role} " +
            "  AND reason_code = #{reasonCode} " +
            "  AND ref_id = #{refId}")
    int countByReasonAndRef(
            @Param("userId") Long userId,
            @Param("role") String role,
            @Param("reasonCode") String reasonCode,
            @Param("refId") Long refId
    );
}