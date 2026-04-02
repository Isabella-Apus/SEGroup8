package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.dto.LoginRequest;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.AuthService;
import com.segroup8.platform.utils.JwtUtils;
import com.segroup8.platform.vo.LoginVO;
import com.segroup8.platform.vo.UserVO;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    public AuthServiceImpl(UserMapper userMapper, JwtUtils jwtUtils) {
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public LoginVO login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .last("limit 1"));
        if (user == null || !user.getPassword().equals(request.getPassword())) {
            throw new BusinessException(401, "Username or password is incorrect");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "Account has been disabled");
        }

        String token = jwtUtils.createToken(user.getId(), user.getUsername(), user.getRole());
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setNickname(user.getNickname());
        userVO.setAvatar(user.getAvatar());
        userVO.setRole(user.getRole());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUser(userVO);
        return loginVO;
    }
}
