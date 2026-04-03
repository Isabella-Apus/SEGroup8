package com.segroup8.platform.service;

import com.segroup8.platform.dto.AdminAuditLogQueryRequest;
import com.segroup8.platform.vo.AdminAuditLogVO;
import com.segroup8.platform.vo.PageVO;

public interface AdminAuditLogService {

    void record(String action, String targetType, Long targetId, String detail);

    PageVO<AdminAuditLogVO> pageLogs(AdminAuditLogQueryRequest request);
}
