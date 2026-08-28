package com.segroup8.platform.service.impl;

import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.RoleEnum;
import com.segroup8.platform.common.UserStatusEnum;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("DOMAIN_A")
@Tag("UC04")
class AdminUserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    private AdminUserServiceImpl adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserServiceImpl(userMapper);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void banUser_shouldUpdateStatusToBanned() {
        UserContext.setUserId(1L);
        User admin = new User();
        admin.setId(1L);
        admin.setRole(RoleEnum.ADMIN.name());

        User target = new User();
        target.setId(2L);
        target.setRole(RoleEnum.USER.name());
        target.setStatus(UserStatusEnum.NORMAL.name());

        when(userMapper.selectById(1L)).thenReturn(admin);
        when(userMapper.selectById(2L)).thenReturn(target);

        adminUserService.banUser(2L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals(UserStatusEnum.BANNED.name(), captor.getValue().getStatus());
    }

    @Test
    void banUser_shouldThrowWhenCurrentUserNotAdmin() {
        UserContext.setUserId(1L);
        User nonAdmin = new User();
        nonAdmin.setId(1L);
        nonAdmin.setRole(RoleEnum.USER.name());
        when(userMapper.selectById(1L)).thenReturn(nonAdmin);

        BusinessException ex = assertThrows(BusinessException.class, () -> adminUserService.banUser(2L));

        assertEquals(403, ex.getCode());
    }

    @Test
    void unbanUser_shouldRestoreNormalStatus() {
        UserContext.setUserId(1L);
        User admin = new User();
        admin.setId(1L);
        admin.setRole(RoleEnum.ADMIN.name());

        User target = new User();
        target.setId(2L);
        target.setStatus(UserStatusEnum.BANNED.name());

        when(userMapper.selectById(1L)).thenReturn(admin);
        when(userMapper.selectById(2L)).thenReturn(target);

        adminUserService.unbanUser(2L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals(UserStatusEnum.NORMAL.name(), captor.getValue().getStatus());
    }

    @Test
    void banUser_shouldRejectSelfBan() {
        UserContext.setUserId(1L);
        User admin = new User();
        admin.setId(1L);
        admin.setRole(RoleEnum.ADMIN.name());
        when(userMapper.selectById(1L)).thenReturn(admin);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.banUser(1L));

        assertEquals(400, ex.getCode());
        verify(userMapper, never()).updateById(org.mockito.ArgumentMatchers.any(User.class));
    }
}
