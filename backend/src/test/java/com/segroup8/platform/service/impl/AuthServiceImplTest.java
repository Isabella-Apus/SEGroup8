package com.segroup8.platform.service.impl;

import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.UserStatusEnum;
import com.segroup8.platform.dto.LoginRequest;
import com.segroup8.platform.dto.RegisterRequest;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.utils.JwtUtils;
import com.segroup8.platform.utils.PasswordUtils;
import com.segroup8.platform.vo.LoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtils jwtUtils;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userMapper, jwtUtils);
    }

    @Test
    void register_shouldEncodePasswordAndInsertUser() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new_user");
        request.setPassword("newPass123");
        request.setNickname("新用户");
        request.setPhone("13800138000");
        request.setEmail("new@demo.com");
        when(userMapper.selectOne(any())).thenReturn(null);

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User inserted = captor.getValue();
        assertEquals("new_user", inserted.getUsername());
        assertEquals("新用户", inserted.getNickname());
        assertTrue(PasswordUtils.matches("newPass123", inserted.getPassword()));
        assertEquals(UserStatusEnum.NORMAL.name(), inserted.getStatus());
    }

    @Test
    void login_shouldUpgradeLegacyPasswordAndReturnToken() {
        User dbUser = new User();
        dbUser.setId(3L);
        dbUser.setUsername("user");
        dbUser.setPassword("user123");
        dbUser.setNickname("DemoUser");
        dbUser.setStatus(UserStatusEnum.NORMAL.name());
        dbUser.setRole("USER");
        when(userMapper.selectOne(any())).thenReturn(dbUser);
        when(jwtUtils.createToken(3L, "user", "USER")).thenReturn("jwt-token");

        LoginRequest request = new LoginRequest();
        request.setUsername("user");
        request.setPassword("user123");

        LoginVO loginVO = authService.login(request);

        assertEquals("jwt-token", loginVO.getToken());
        assertEquals("USER", loginVO.getRole());
        assertNotNull(loginVO.getUser());

        ArgumentCaptor<User> updateCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(updateCaptor.capture());
        assertEquals(3L, updateCaptor.getValue().getId());
        assertTrue(updateCaptor.getValue().getPassword().startsWith("$2"));
    }

    @Test
    void login_shouldThrowWhenPasswordInvalid() {
        User dbUser = new User();
        dbUser.setId(3L);
        dbUser.setUsername("user");
        dbUser.setPassword(PasswordUtils.encode("right-pass"));
        dbUser.setStatus(UserStatusEnum.NORMAL.name());
        dbUser.setRole("USER");
        when(userMapper.selectOne(any())).thenReturn(dbUser);

        LoginRequest request = new LoginRequest();
        request.setUsername("user");
        request.setPassword("wrong-pass");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals(401, ex.getCode());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void register_shouldRejectDuplicateUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing-user");
        request.setPassword("newPass123");
        User existing = new User();
        existing.setUsername("existing-user");
        when(userMapper.selectOne(any())).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(request));

        assertEquals(400, ex.getCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void login_shouldRejectBannedUser() {
        User dbUser = new User();
        dbUser.setId(3L);
        dbUser.setUsername("banned-user");
        dbUser.setPassword(PasswordUtils.encode("right-pass"));
        dbUser.setStatus(UserStatusEnum.BANNED.name());
        dbUser.setRole("USER");
        when(userMapper.selectOne(any())).thenReturn(dbUser);

        LoginRequest request = new LoginRequest();
        request.setUsername("banned-user");
        request.setPassword("right-pass");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(request));

        assertEquals(403, ex.getCode());
        verify(jwtUtils, never()).createToken(any(), any(), any());
    }
}
