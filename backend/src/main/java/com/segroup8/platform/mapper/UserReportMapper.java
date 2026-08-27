package com.segroup8.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.segroup8.platform.entity.UserReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface UserReportMapper extends BaseMapper<UserReport> {

    /**
     * 分页查询举报列表（管理员用）
     * status = null 时查全部
     */
    @Select("<script>" +
            "SELECT * FROM user_report" +
            "<where>" +
            "  <if test='status != null'> AND status = #{status} </if>" +
            "  <if test='reportedId != null'> AND reported_id = #{reportedId} </if>" +
            "  <if test='reporterId != null'> AND reporter_id = #{reporterId} </if>" +
            "</where>" +
            " ORDER BY create_time DESC" +
            "</script>")
    IPage<UserReport> pageReports(
            Page<UserReport> page,
            @Param("status") Integer status,
            @Param("reportedId") Long reportedId,
            @Param("reporterId") Long reporterId
    );

    /**
     * 查询某人对另一人是否已经有待审核的举报
     * 只阻止重复提交待审核的举报，已处理的举报不影响新举报
     */
    @Select("SELECT COUNT(*) FROM user_report " +
            "WHERE reporter_id = #{reporterId} " +
            "  AND reported_id = #{reportedId} " +
            "  AND status = 0")
    int countActiveReport(
            @Param("reporterId") Long reporterId,
            @Param("reportedId") Long reportedId
    );

    /**
     * 统计某用户近2年内被判定成立的举报数（用于信用分计算）
     */
    @Select("SELECT COUNT(*) FROM user_report " +
            "WHERE reported_id = #{userId} " +
            "  AND status = 1 " +
            "  AND audit_time >= #{cutoff}")
    int countUpheldReportsIn2Years(
            @Param("userId") Long userId,
            @Param("cutoff") LocalDateTime cutoff);
}
