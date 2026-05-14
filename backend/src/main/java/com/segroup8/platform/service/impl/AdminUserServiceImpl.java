package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.RoleEnum;
import com.segroup8.platform.common.UserStatusEnum;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.AdminUserQueryRequest;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.AdminUserService;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;

    public AdminUserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public PageVO<UserVO> pageUsers(AdminUserQueryRequest request) {
        assertAdmin();
        Page<User> page = userMapper.selectPage(
                Page.of(request.getPageNum(), request.getPageSize()),
                buildQuery(request));

        List<UserVO> records = page.getRecords().stream().map(this::toUserVO).toList();
        PageVO<UserVO> result = new PageVO<>();
        result.setTotal(page.getTotal());
        result.setPageNum(request.getPageNum());
        result.setPageSize(request.getPageSize());
        result.setRecords(records);
        return result;
    }

    @Override
    public void banUser(Long userId) {
        Long currentUserId = UserContext.getUserId();
        assertAdmin();
        if (Objects.equals(currentUserId, userId)) {
            throw new BusinessException(400, "管理员不能封禁自己");
        }
        updateUserStatus(userId, UserStatusEnum.BANNED.name());
    }

    @Override
    public void unbanUser(Long userId) {
        assertAdmin();
        updateUserStatus(userId, UserStatusEnum.NORMAL.name());
    }

    private void updateUserStatus(Long userId, String status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    private LambdaQueryWrapper<User> buildQuery(AdminUserQueryRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getId);
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w
                    .like(User::getUsername, request.getKeyword())
                    .or()
                    .like(User::getNickname, request.getKeyword()));
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(User::getStatus, request.getStatus());
        }
        return wrapper;
    }

    private void assertAdmin() {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "未登录");
        }
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null || !Objects.equals(currentUser.getRole(), RoleEnum.ADMIN.name())) {
            throw new BusinessException(403, "无管理员权限");
        }
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreditScore(user.getCreditScore());
        return vo;
    }
}
