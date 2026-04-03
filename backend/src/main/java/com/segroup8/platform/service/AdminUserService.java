package com.segroup8.platform.service;

import com.segroup8.platform.dto.AdminUserQueryRequest;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.UserVO;

public interface AdminUserService {

    PageVO<UserVO> pageUsers(AdminUserQueryRequest request);

    void banUser(Long userId);

    void unbanUser(Long userId);
}
