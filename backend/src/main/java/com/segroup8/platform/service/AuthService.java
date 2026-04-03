package com.segroup8.platform.service;

import com.segroup8.platform.dto.LoginRequest;
import com.segroup8.platform.dto.RegisterRequest;
import com.segroup8.platform.vo.LoginVO;

public interface AuthService {

    void register(RegisterRequest request);

    LoginVO login(LoginRequest request);
}
