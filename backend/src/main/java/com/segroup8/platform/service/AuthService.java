package com.segroup8.platform.service;

import com.segroup8.platform.dto.LoginRequest;
import com.segroup8.platform.vo.LoginVO;

public interface AuthService {

    LoginVO login(LoginRequest request);
}
