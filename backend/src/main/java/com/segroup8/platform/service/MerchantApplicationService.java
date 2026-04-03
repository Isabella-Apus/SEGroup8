package com.segroup8.platform.service;

import com.segroup8.platform.dto.AdminMerchantApplicationQueryRequest;
import com.segroup8.platform.dto.MerchantApplicationRejectRequest;
import com.segroup8.platform.dto.MerchantApplicationSubmitRequest;
import com.segroup8.platform.vo.MerchantApplicationVO;
import com.segroup8.platform.vo.PageVO;

public interface MerchantApplicationService {

    void submit(MerchantApplicationSubmitRequest request);

    MerchantApplicationVO getMyApplication();

    PageVO<MerchantApplicationVO> pageForAdmin(AdminMerchantApplicationQueryRequest request);

    void approve(Long applicationId);

    void reject(Long applicationId, MerchantApplicationRejectRequest request);
}
