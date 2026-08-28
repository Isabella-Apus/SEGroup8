package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.dto.SecondhandOrderCreateRequest;
import com.segroup8.platform.dto.SecondhandProductPageQueryRequest;
import com.segroup8.platform.dto.SecondhandProductSaveRequest;
import com.segroup8.platform.dto.SecondhandProductStatusUpdateRequest;
import com.segroup8.platform.service.SearchBehaviorService;
import com.segroup8.platform.service.SecondhandProductService;
import com.segroup8.platform.vo.OrderVO;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.SecondhandSellerPublicVO;
import com.segroup8.platform.vo.SecondhandProductVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/secondhand")
public class SecondhandProductController {

    private final SecondhandProductService secondhandProductService;
    private final SearchBehaviorService searchBehaviorService;

    public SecondhandProductController(SecondhandProductService secondhandProductService,
            SearchBehaviorService searchBehaviorService) {
        this.secondhandProductService = secondhandProductService;
        this.searchBehaviorService = searchBehaviorService;
    }

    @Operation(summary = "分页查询二手在售商品")
    @GetMapping("/list")
    public Result<PageVO<SecondhandProductVO>> list(@Valid @ModelAttribute SecondhandProductPageQueryRequest request) {
        searchBehaviorService.recordKeyword(request.getKeyword());
        return Result.success(secondhandProductService.pagePublicProducts(request));
    }

    @Operation(summary = "获取二手商品详情")
    @GetMapping("/detail/{productId}")
    public Result<SecondhandProductVO> detail(@PathVariable Long productId) {
        return Result.success(secondhandProductService.getPublicProductDetail(productId));
    }

    @Operation(summary = "公开二手卖家信息")
    @GetMapping("/seller-public/{sellerUserId}")
    public Result<SecondhandSellerPublicVO> publicSeller(@PathVariable Long sellerUserId) {
        return Result.success(secondhandProductService.getPublicSeller(sellerUserId));
    }

    @Operation(summary = "公开二手卖家在售商品")
    @GetMapping("/seller-public/{sellerUserId}/products")
    public Result<PageVO<SecondhandProductVO>> publicSellerProducts(
            @PathVariable Long sellerUserId,
            @Valid @ModelAttribute SecondhandProductPageQueryRequest request) {
        return Result.success(secondhandProductService.pagePublicSellerProducts(sellerUserId, request));
    }

    @Operation(summary = "卖家分页查询我的二手商品")
    @GetMapping("/seller/list")
    public Result<PageVO<SecondhandProductVO>> sellerList(
            @Valid @ModelAttribute SecondhandProductPageQueryRequest request) {
        return Result.success(secondhandProductService.pageSellerProducts(request));
    }

    @Operation(summary = "卖家发布二手商品")
    @PostMapping("/seller")
    public Result<SecondhandProductVO> sellerCreate(@Valid @RequestBody SecondhandProductSaveRequest request) {
        return Result.success(secondhandProductService.createSellerProduct(request));
    }

    @Operation(summary = "卖家编辑二手商品")
    @PutMapping("/seller/{productId}")
    public Result<SecondhandProductVO> sellerUpdate(@PathVariable Long productId,
            @Valid @RequestBody SecondhandProductSaveRequest request) {
        return Result.success(secondhandProductService.updateSellerProduct(productId, request));
    }

    @Operation(summary = "卖家删除二手商品")
    @DeleteMapping("/seller/{productId}")
    public Result<Void> sellerDelete(@PathVariable Long productId) {
        secondhandProductService.deleteSellerProduct(productId);
        return Result.success();
    }

    @Operation(summary = "卖家切换二手商品上下架")
    @PostMapping("/seller/{productId}/status")
    public Result<SecondhandProductVO> sellerChangeStatus(@PathVariable Long productId,
            @Valid @RequestBody SecondhandProductStatusUpdateRequest request) {
        return Result.success(secondhandProductService.changeSellerProductStatus(productId, request.getStatus()));
    }

    @Operation(summary = "购买二手商品")
    @PostMapping("/{productId}/buy")
    public Result<OrderVO> buy(@PathVariable Long productId,
            @Valid @RequestBody SecondhandOrderCreateRequest request) {
        return Result.success(secondhandProductService.buySecondhandProduct(productId, request));
    }
}
