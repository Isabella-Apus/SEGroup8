package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.RoleEnum;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.AdminAuditLogQueryRequest;
import com.segroup8.platform.entity.AdminAuditLog;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.AdminAuditLogMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.AdminAuditLogService;
import com.segroup8.platform.vo.AdminAuditLogVO;
import com.segroup8.platform.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class AdminAuditLogServiceImpl implements AdminAuditLogService {

    private final AdminAuditLogMapper adminAuditLogMapper;
    private final UserMapper userMapper;

    public AdminAuditLogServiceImpl(AdminAuditLogMapper adminAuditLogMapper, UserMapper userMapper) {
        this.adminAuditLogMapper = adminAuditLogMapper;
        this.userMapper = userMapper;
    }

    @Override
    public void record(String action, String targetType, Long targetId, String detail) {
        Long adminUserId = UserContext.getUserId();
        User admin = adminUserId == null ? null : userMapper.selectById(adminUserId);

        AdminAuditLog auditLog = new AdminAuditLog();
        auditLog.setAdminUserId(adminUserId);
        auditLog.setAdminUsername(admin == null ? "UNKNOWN" : admin.getUsername());
        auditLog.setAction(action);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);
        auditLog.setDetail(detail);
        auditLog.setCreateTime(LocalDateTime.now());
        adminAuditLogMapper.insert(auditLog);
    }

    @Override
    public PageVO<AdminAuditLogVO> pageLogs(AdminAuditLogQueryRequest request) {
        assertAdmin();
        LambdaQueryWrapper<AdminAuditLog> wrapper = new LambdaQueryWrapper<AdminAuditLog>()
                .orderByDesc(AdminAuditLog::getId);

        if (StringUtils.hasText(request.getAction())) {
            wrapper.eq(AdminAuditLog::getAction, request.getAction());
        }
        if (StringUtils.hasText(request.getTargetType())) {
            wrapper.eq(AdminAuditLog::getTargetType, request.getTargetType());
        }
        if (StringUtils.hasText(request.getAdminUsername())) {
            wrapper.like(AdminAuditLog::getAdminUsername, request.getAdminUsername());
        }

        Page<AdminAuditLog> page = adminAuditLogMapper.selectPage(Page.of(request.getPageNum(), request.getPageSize()),
                wrapper);
        List<AdminAuditLogVO> records = page.getRecords().stream().map(this::toVO).toList();

        PageVO<AdminAuditLogVO> result = new PageVO<>();
        result.setTotal(page.getTotal());
        result.setPageNum(request.getPageNum());
        result.setPageSize(request.getPageSize());
        result.setRecords(records);
        return result;
    }

    private AdminAuditLogVO toVO(AdminAuditLog log) {
        AdminAuditLogVO vo = new AdminAuditLogVO();
        vo.setId(log.getId());
        vo.setAdminUserId(log.getAdminUserId());
        vo.setAdminUsername(log.getAdminUsername());
        vo.setAction(log.getAction());
        vo.setTargetType(log.getTargetType());
        vo.setTargetId(log.getTargetId());
        vo.setDetail(log.getDetail());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    private void assertAdmin() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getRole(), RoleEnum.ADMIN.name())) {
            throw new BusinessException(403, "无管理员权限");
        }
    }
}
