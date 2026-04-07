package com.segroup8.platform.service;

import com.segroup8.platform.dto.ProductPageQueryRequest;
import com.segroup8.platform.dto.ProductSaveRequest;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.ProductVO;

public interface ProductService {

    PageVO<ProductVO> pagePublicProducts(ProductPageQueryRequest request);

    ProductVO getPublicProductDetail(Long productId);

    PageVO<ProductVO> pageSellerProducts(ProductPageQueryRequest request);

    ProductVO createSellerProduct(ProductSaveRequest request);

    ProductVO updateSellerProduct(Long productId, ProductSaveRequest request);

    void deleteSellerProduct(Long productId);

    ProductVO changeSellerProductStatus(Long productId, Integer status);

    ProductVO adjustSellerProductStock(Long productId, Integer delta);
}
