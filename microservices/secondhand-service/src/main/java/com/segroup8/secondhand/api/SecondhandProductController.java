package com.segroup8.secondhand.api;

import static com.segroup8.secondhand.security.AuthenticationSupport.requireUser;

import com.segroup8.secondhand.common.ApiResponse;
import com.segroup8.secondhand.common.PageResponse;
import com.segroup8.secondhand.security.AuthenticatedUser;
import com.segroup8.secondhand.service.ProductApplicationService;
import com.segroup8.secondhand.service.TradeApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/secondhand")
public class SecondhandProductController {
    private final ProductApplicationService products;
    private final TradeApplicationService trades;

    public SecondhandProductController(ProductApplicationService products, TradeApplicationService trades) {
        this.products = products;
        this.trades = trades;
    }

    @Operation(summary = "UC16-UC19 分页查询公开二手商品")
    @GetMapping("/list")
    ApiResponse<PageResponse<ProductView>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String conditionLevel,
            @RequestParam(required = false) Integer isNegotiable) {
        return ApiResponse.success(products.listPublic(pageNum, pageSize, keyword, categoryId, minPrice,
                maxPrice, conditionLevel, isNegotiable, sortBy));
    }

    @Operation(summary = "UC16-UC19 获取二手商品详情")
    @GetMapping("/detail/{productId}")
    ApiResponse<ProductView> detail(@PathVariable long productId) {
        return ApiResponse.success(products.publicDetail(productId));
    }

    @Operation(summary = "UC16 公开二手卖家信息")
    @GetMapping("/seller-public/{sellerUserId}")
    ApiResponse<SellerPublicView> publicSeller(@PathVariable long sellerUserId) {
        return ApiResponse.success(products.sellerPublic(sellerUserId));
    }

    @Operation(summary = "UC16 公开二手卖家在售商品")
    @GetMapping("/seller-public/{sellerUserId}/products")
    ApiResponse<PageResponse<ProductView>> publicSellerProducts(@PathVariable long sellerUserId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String conditionLevel,
            @RequestParam(required = false) Integer isNegotiable) {
        return ApiResponse.success(products.listPublicSeller(sellerUserId, pageNum, pageSize, keyword,
                categoryId, minPrice, maxPrice, conditionLevel, isNegotiable, sortBy));
    }

    @Operation(summary = "UC16 卖家查看自己的二手商品")
    @GetMapping("/seller/list")
    ApiResponse<PageResponse<ProductView>> sellerList(HttpServletRequest request,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String conditionLevel,
            @RequestParam(required = false) Integer isNegotiable,
            @RequestParam(required = false) Integer status) {
        long sellerId = requireUser(request).userId();
        return ApiResponse.success(products.listSeller(sellerId, pageNum, pageSize, keyword, categoryId,
                minPrice, maxPrice, conditionLevel, isNegotiable, status, sortBy));
    }

    @Operation(summary = "UC16 发布二手商品")
    @PostMapping("/seller")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ProductView> create(HttpServletRequest request, @Valid @RequestBody ProductSaveRequest command) {
        AuthenticatedUser user = requireUser(request);
        return ApiResponse.success(products.create(user.userId(), user.username(), command));
    }

    @Operation(summary = "UC16 编辑二手商品")
    @PutMapping("/seller/{productId}")
    ApiResponse<ProductView> update(HttpServletRequest request, @PathVariable long productId,
            @Valid @RequestBody ProductSaveRequest command) {
        return ApiResponse.success(products.update(requireUser(request).userId(), productId, command));
    }

    @Operation(summary = "UC16 删除二手商品")
    @DeleteMapping("/seller/{productId}")
    ApiResponse<Void> delete(HttpServletRequest request, @PathVariable long productId) {
        products.delete(requireUser(request).userId(), productId);
        return ApiResponse.success();
    }

    @Operation(summary = "UC16 上下架二手商品")
    @PostMapping("/seller/{productId}/status")
    ApiResponse<ProductView> status(HttpServletRequest request, @PathVariable long productId,
            @Valid @RequestBody ProductStatusRequest command) {
        return ApiResponse.success(products.changeStatus(requireUser(request).userId(), productId, command.status()));
    }

    @Operation(summary = "UC17 直接购买并幂等请求 order-service 创建待付款订单")
    @PostMapping("/{productId}/buy")
    ResponseEntity<ApiResponse<TradeOrderView>> buy(HttpServletRequest request, @PathVariable long productId,
            @Valid @RequestBody BuyRequest command) {
        TradeOrderView view = trades.buy(requireUser(request).userId(), productId, command.addressId(), command.remark());
        HttpStatus status = ListStatus.isProcessing(view.requestStatus()) ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponse.success(view));
    }

    private static final class ListStatus {
        static boolean isProcessing(String status) {
            return "PENDING".equals(status) || "RETRY".equals(status);
        }
    }
}
