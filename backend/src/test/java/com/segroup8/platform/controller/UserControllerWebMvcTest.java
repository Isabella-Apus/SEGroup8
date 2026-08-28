package com.segroup8.platform.controller;

import com.segroup8.platform.common.GlobalExceptionHandler;
import com.segroup8.platform.service.UserService;
import com.segroup8.platform.vo.AddressVO;
import com.segroup8.platform.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("DOMAIN_A")
@Tag("UC02")
class UserControllerWebMvcTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .build();
    }

    @Test
    void profile_shouldReturnCurrentUser() throws Exception {
        UserVO user = new UserVO();
        user.setId(10L);
        user.setUsername("member-a");
        user.setNickname("Member A");
        when(userService.getCurrentUserProfile()).thenReturn(user);

        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.username").value("member-a"));
    }

    @Test
    void createAddress_shouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/user/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverName\":\"Test User\",\"receiverPhone\":\"13800138000\",\"province\":\"Beijing\",\"city\":\"Beijing\",\"detailAddress\":\"No.1 Test Road\",\"isDefault\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        verify(userService).createAddress(any());
    }

    @Test
    void createAddress_shouldRejectInvalidPhone() throws Exception {
        mockMvc.perform(post("/api/user/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverName\":\"Test User\",\"receiverPhone\":\"123\",\"province\":\"Beijing\",\"city\":\"Beijing\",\"detailAddress\":\"No.1 Test Road\",\"isDefault\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(userService, never()).createAddress(any());
    }

    @Test
    void listAddresses_shouldReturnOnlyServiceResult() throws Exception {
        AddressVO address = new AddressVO();
        address.setId(20L);
        address.setReceiverName("Test User");
        when(userService.listMyAddresses()).thenReturn(List.of(address));

        mockMvc.perform(get("/api/user/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(20))
                .andExpect(jsonPath("$.data[0].receiverName").value("Test User"));
    }

    @Test
    void deleteAddress_shouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/user/addresses/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(userService).deleteAddress(eq(20L));
    }
}
