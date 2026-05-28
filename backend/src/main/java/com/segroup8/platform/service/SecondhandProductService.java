package com.segroup8.platform.service;

import com.segroup8.platform.dto.SecondhandOrderCreateRequest;
import com.segroup8.platform.dto.SecondhandProductPageQueryRequest;
import com.segroup8.platform.dto.SecondhandProductSaveRequest;
import com.segroup8.platform.vo.OrderVO;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.SecondhandSellerPublicVO;
import com.segroup8.platform.vo.SecondhandProductVO;

public interface SecondhandProductService {

    PageVO<SecondhandProductVO> pagePublicProducts(SecondhandProductPageQueryRequest request);

    PageVO<SecondhandProductVO> pagePublicSellerProducts(Long sellerUserId, SecondhandProductPageQueryRequest request);

    SecondhandSellerPublicVO getPublicSeller(Long sellerUserId);

    SecondhandProductVO getPublicProductDetail(Long productId);

    PageVO<SecondhandProductVO> pageSellerProducts(SecondhandProductPageQueryRequest request);

    SecondhandProductVO createSellerProduct(SecondhandProductSaveRequest request);

    SecondhandProductVO updateSellerProduct(Long productId, SecondhandProductSaveRequest request);

    void deleteSellerProduct(Long productId);

    SecondhandProductVO changeSellerProductStatus(Long productId, Integer status);

    OrderVO buySecondhandProduct(Long productId, SecondhandOrderCreateRequest request);
}

