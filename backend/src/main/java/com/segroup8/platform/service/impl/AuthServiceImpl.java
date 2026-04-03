package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.RoleEnum;
import com.segroup8.platform.common.UserStatusEnum;
import com.segroup8.platform.dto.LoginRequest;
import com.segroup8.platform.dto.RegisterRequest;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.AuthService;
import com.segroup8.platform.utils.JwtUtils;
import com.segroup8.platform.utils.PasswordUtils;
import com.segroup8.platform.vo.LoginVO;
import com.segroup8.platform.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    public AuthServiceImpl(UserMapper userMapper, JwtUtils jwtUtils) {
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public void register(RegisterRequest request) {
        User exists = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .last("limit 1"));
        if (exists != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(PasswordUtils.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setAvatar("");
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRole(RoleEnum.USER.name());
        user.setStatus(UserStatusEnum.NORMAL.name());
        user.setCreditScore(100);
        userMapper.insert(user);
    }

    @Override
    public LoginVO login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .last("limit 1"));
        if (user == null || !PasswordUtils.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!Objects.equals(user.getStatus(), UserStatusEnum.NORMAL.name())) {
            throw new BusinessException(403, "账号已禁用");
        }

        if (StringUtils.hasText(user.getPassword()) && !user.getPassword().startsWith("$2")) {
            User update = new User();
            update.setId(user.getId());
            update.setPassword(PasswordUtils.encode(request.getPassword()));
            userMapper.updateById(update);
        }

        String token = jwtUtils.createToken(user.getId(), user.getUsername(), user.getRole());
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setNickname(user.getNickname());
        userVO.setAvatar(user.getAvatar());
        userVO.setPhone(user.getPhone());
        userVO.setEmail(user.getEmail());
        userVO.setRole(user.getRole());
        userVO.setStatus(user.getStatus());
        userVO.setCreditScore(user.getCreditScore());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setRole(user.getRole());
        loginVO.setUser(userVO);
        return loginVO;
    }
}
