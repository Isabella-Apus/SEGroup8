package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.entity.Product;
import com.segroup8.platform.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "获取商品列表")
    @GetMapping("/list")
    public Result<List<Product>> list() {
        return Result.success(productService.listProducts());
    }
}
