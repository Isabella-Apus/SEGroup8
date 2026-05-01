package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.dto.ProductPageQueryRequest;
import com.segroup8.platform.dto.ProductSaveRequest;
import com.segroup8.platform.dto.ProductStatusUpdateRequest;
import com.segroup8.platform.dto.ProductStockAdjustRequest;
import com.segroup8.platform.entity.Product;
import com.segroup8.platform.service.ProductService;
import com.segroup8.platform.service.SearchService;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;
    private final SearchService searchService;

    public ProductController(ProductService productService, SearchService searchService) {
        this.productService = productService;
        this.searchService = searchService;
    }

    @Operation(summary = "分页查询在售商品")
    @GetMapping("/list")
    public Result<PageVO<ProductVO>> list(@Valid @ModelAttribute ProductPageQueryRequest request) {
        return Result.success(productService.pagePublicProducts(request));
    }

    @Operation(summary = "获取商品详情")
    @GetMapping("/detail/{productId}")
    public Result<ProductVO> detail(@PathVariable Long productId) {
        return Result.success(productService.getPublicProductDetail(productId));
    }

    @Operation(summary = "商品搜索")
    @GetMapping("/search")
    public List<Product> search(String keyword,
            @RequestParam(defaultValue = "0.3") Double threshold) {
        return searchService.search(keyword, threshold);
    }

    @Operation(summary = "卖家分页查询商品")
    @GetMapping("/seller/list")
    public Result<PageVO<ProductVO>> sellerList(@Valid @ModelAttribute ProductPageQueryRequest request) {
        return Result.success(productService.pageSellerProducts(request));
    }

    @Operation(summary = "卖家新增商品")
    @PostMapping("/seller")
    public Result<ProductVO> sellerCreate(@Valid @RequestBody ProductSaveRequest request) {
        return Result.success(productService.createSellerProduct(request));
    }

    @Operation(summary = "卖家更新商品")
    @PutMapping("/seller/{productId}")
    public Result<ProductVO> sellerUpdate(@PathVariable Long productId,
            @Valid @RequestBody ProductSaveRequest request) {
        return Result.success(productService.updateSellerProduct(productId, request));
    }

    @Operation(summary = "卖家删除商品")
    @DeleteMapping("/seller/{productId}")
    public Result<Void> sellerDelete(@PathVariable Long productId) {
        productService.deleteSellerProduct(productId);
        return Result.success();
    }

    @Operation(summary = "卖家切换商品上下架")
    @PostMapping("/seller/{productId}/status")
    public Result<ProductVO> sellerChangeStatus(@PathVariable Long productId,
            @Valid @RequestBody ProductStatusUpdateRequest request) {
        return Result.success(productService.changeSellerProductStatus(productId, request.getStatus()));
    }

    @Operation(summary = "卖家调整商品库存")
    @PostMapping("/seller/{productId}/stock/adjust")
    public Result<ProductVO> sellerAdjustStock(@PathVariable Long productId,
            @Valid @RequestBody ProductStockAdjustRequest request) {
        return Result.success(productService.adjustSellerProductStock(productId, request.getDelta()));
    }
}
