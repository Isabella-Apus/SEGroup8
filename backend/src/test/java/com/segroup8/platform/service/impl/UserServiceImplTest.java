package com.segroup8.platform.service.impl;

import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.AddressSaveRequest;
import com.segroup8.platform.entity.Address;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.AddressMapper;
import com.segroup8.platform.mapper.MerchantApplicationMapper;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.MerchantApplicationService;
import com.segroup8.platform.vo.UserVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private AddressMapper addressMapper;

    @Mock
    private MerchantApplicationService merchantApplicationService;

    @Mock
    private ShopMapper shopMapper;

    @Mock
    private MerchantApplicationMapper merchantApplicationMapper;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userMapper,
                addressMapper,
                merchantApplicationService,
                shopMapper,
                merchantApplicationMapper);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void getCurrentUserProfile_shouldMapUserInfo() {
        UserContext.setUserId(10L);
        User user = new User();
        user.setId(10L);
        user.setUsername("user");
        user.setNickname("DemoUser");
        user.setRole("USER");
        when(userMapper.selectById(10L)).thenReturn(user);

        UserVO vo = userService.getCurrentUserProfile();

        assertEquals(10L, vo.getId());
        assertEquals("user", vo.getUsername());
        assertEquals("DemoUser", vo.getNickname());
    }

    @Test
    void createAddress_whenDefault_shouldClearPreviousDefault() {
        UserContext.setUserId(10L);
        AddressSaveRequest request = new AddressSaveRequest();
        request.setReceiverName("张三");
        request.setReceiverPhone("13800138000");
        request.setProvince("北京市");
        request.setCity("北京市");
        request.setDetailAddress("软件园");
        request.setIsDefault(1);

        userService.createAddress(request);

        verify(addressMapper).update(any(), any());
        ArgumentCaptor<Address> insertCaptor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).insert(insertCaptor.capture());
        assertEquals(10L, insertCaptor.getValue().getUserId());
        assertEquals(1, insertCaptor.getValue().getIsDefault());
    }

    @Test
    void deleteAddress_shouldThrowWhenAddressNotOwned() {
        UserContext.setUserId(10L);
        Address address = new Address();
        address.setId(99L);
        address.setUserId(11L);
        when(addressMapper.selectById(99L)).thenReturn(address);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.deleteAddress(99L));

        assertEquals(404, ex.getCode());
    }
}
