package com.segroup8.catalog;

import com.segroup8.catalog.CatalogService.Product;
import com.segroup8.catalog.CatalogService.ProductCommand;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    private final CatalogService service;
    public CatalogController(CatalogService service) { this.service = service; }

    @Operation(summary = "UC06 商品搜索与筛选")
    @GetMapping("/products")
    List<Product> search(@RequestParam(required=false) String keyword, @RequestParam(required=false) String category,
            @RequestParam(required=false) Long shopId,
            @RequestParam(required=false) BigDecimal minPrice, @RequestParam(required=false) BigDecimal maxPrice,
            @RequestParam(defaultValue="newest") String sort) {
        return service.search(keyword, category, shopId, minPrice, maxPrice, sort);
    }
    @Operation(summary = "UC06 商品详情")
    @GetMapping("/products/{id}") Product detail(@PathVariable long id) { return service.publicDetail(id); }

    @Operation(summary = "UC07 卖家商品列表")
    @GetMapping("/seller/products")
    List<Product> sellerList(@RequestHeader("X-Seller-Id") long sellerId) {
        return service.sellerProducts(sellerId);
    }

    @Operation(summary = "UC07 新建商品草稿")
    @PostMapping("/seller/products")
    @ResponseStatus(HttpStatus.CREATED)
    Product create(@RequestHeader("X-Seller-Id") long sellerId, @Valid @RequestBody ProductRequest request) {
        return service.create(sellerId, request.command());
    }

    @Operation(summary = "UC07 编辑商品")
    @PutMapping("/seller/products/{id}")
    Product update(@RequestHeader("X-Seller-Id") long sellerId, @PathVariable long id,
            @Valid @RequestBody ProductRequest request) {
        return service.update(sellerId, id, request.command());
    }

    @Operation(summary = "UC07 商品生命周期动作")
    @PostMapping("/seller/products/{id}/actions/{action}")
    Product transition(@RequestHeader("X-Seller-Id") long sellerId, @PathVariable long id,
            @PathVariable String action) {
        return service.transition(sellerId, id, action.toUpperCase());
    }

    @Operation(summary = "UC07 应用商品审核决定")
    @PostMapping("/internal/products/{id}/risk-decision")
    Product riskDecision(@PathVariable long id, @RequestBody RiskDecision request) {
        return service.applyRiskDecision(id, request.approved());
    }

    public record ProductRequest(@NotNull @Min(1) Long shopId, @NotBlank String name, String description,
            @NotBlank String category, @NotNull @DecimalMin("0.01") BigDecimal price, @Min(0) int stock) {
        ProductCommand command() {
            return new ProductCommand(shopId, name, description, category, price, stock);
        }
    }

    public record RiskDecision(boolean approved) {}
}

@org.springframework.web.bind.annotation.RestControllerAdvice
class CatalogErrorHandler {
    @ExceptionHandler(DomainException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String,Object> domain(DomainException e) { return Map.of("code", e.code, "message", e.getMessage()); }
}
