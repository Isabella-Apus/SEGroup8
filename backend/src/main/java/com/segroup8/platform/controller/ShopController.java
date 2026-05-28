package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.dto.ProductPageQueryRequest;
import com.segroup8.platform.dto.ShopDecorationSaveRequest;
import com.segroup8.platform.service.ProductService;
import com.segroup8.platform.service.ShopService;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.ProductVO;
import com.segroup8.platform.vo.ShopPublicVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopService shopService;
    private final ProductService productService;

    public ShopController(ShopService shopService, ProductService productService) {
        this.shopService = shopService;
        this.productService = productService;
    }

    @Operation(summary = "公开店铺信息")
    @GetMapping("/public/{shopId}")
    public Result<ShopPublicVO> publicShop(@PathVariable Long shopId) {
        return Result.success(shopService.getPublicShop(shopId));
    }

    @Operation(summary = "公开店铺商品")
    @GetMapping("/public/{shopId}/products")
    public Result<PageVO<ProductVO>> publicShopProducts(
            @PathVariable Long shopId,
            @Valid @ModelAttribute ProductPageQueryRequest request) {
        return Result.success(productService.pagePublicShopProducts(shopId, request));
    }

    @Operation(summary = "卖家获取自己的店铺")
    @GetMapping("/seller/current")
    public Result<ShopPublicVO> currentSellerShop() {
        return Result.success(shopService.getCurrentSellerShop());
    }

    @Operation(summary = "卖家保存并发布店铺装修")
    @PutMapping("/seller/decoration")
    public Result<ShopPublicVO> saveDecoration(@Valid @RequestBody ShopDecorationSaveRequest request) {
        return Result.success(shopService.saveCurrentSellerDecoration(request));
    }
}
