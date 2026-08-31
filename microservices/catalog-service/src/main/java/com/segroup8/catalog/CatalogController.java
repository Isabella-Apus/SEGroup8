package com.segroup8.catalog;

import com.segroup8.catalog.CatalogService.Product;
import io.swagger.v3.oas.annotations.Operation;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}

@org.springframework.web.bind.annotation.RestControllerAdvice
class CatalogErrorHandler {
    @ExceptionHandler(DomainException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String,Object> domain(DomainException e) { return Map.of("code", e.code, "message", e.getMessage()); }
}
