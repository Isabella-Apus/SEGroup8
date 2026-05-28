package com.segroup8.platform.service;

import com.segroup8.platform.dto.ShopDecorationSaveRequest;
import com.segroup8.platform.vo.ShopPublicVO;

public interface ShopService {

    ShopPublicVO getPublicShop(Long shopId);

    ShopPublicVO getCurrentSellerShop();

    ShopPublicVO saveCurrentSellerDecoration(ShopDecorationSaveRequest request);
}
