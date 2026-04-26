package com.segroup8.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.segroup8.platform.entity.UserBlock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

@Mapper
public interface UserBlockMapper extends BaseMapper<UserBlock> {

    /**
     * 查询是否已拉黑
     */
    @Select("SELECT COUNT(*) FROM user_block " +
            "WHERE blocker_id = #{blockerId} AND blocked_id = #{blockedId}")
    int isBlocked(
            @Param("blockerId") Long blockerId,
            @Param("blockedId") Long blockedId
    );

    /**
     * 取消拉黑
     */
    @Delete("DELETE FROM user_block " +
            "WHERE blocker_id = #{blockerId} AND blocked_id = #{blockedId}")
    int unblock(
            @Param("blockerId") Long blockerId,
            @Param("blockedId") Long blockedId
    );

    /**
     * 查询某用户拉黑的所有用户ID列表
     */
    @Select("SELECT blocked_id FROM user_block WHERE blocker_id = #{blockerId}")
    List<Long> listBlockedIds(@Param("blockerId") Long blockerId);

    /**
     * 查询拉黑我的所有用户ID列表
     */
    @Select("SELECT blocker_id FROM user_block WHERE blocked_id = #{blockedId}")
    List<Long> listBlockerIds(@Param("blockedId") Long blockedId);

    /**
     * 查询完整拉黑列表（含时间，用于前端展示）
     */
    @Select("SELECT * FROM user_block WHERE blocker_id = #{blockerId} ORDER BY create_time DESC")
    List<UserBlock> listMyBlocks(@Param("blockerId") Long blockerId);
}