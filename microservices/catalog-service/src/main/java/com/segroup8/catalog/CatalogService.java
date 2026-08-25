package com.segroup8.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {
    private final JdbcClient db;

    public CatalogService(JdbcClient db) {
        this.db = db;
    }

    public List<Product> search(String keyword, String category, Long shopId, BigDecimal minPrice,
            BigDecimal maxPrice, String sort) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        Comparator<Product> order = switch (sort == null ? "newest" : sort) {
            case "priceAsc" -> Comparator.comparing(Product::price);
            case "priceDesc" -> Comparator.comparing(Product::price).reversed();
            default -> Comparator.comparing(Product::updatedAt).reversed();
        };
        return db.sql("select * from products where status='ON_SALE'").query(Product.class).list().stream()
                .filter(p -> normalized.isBlank() || p.name().toLowerCase(Locale.ROOT).contains(normalized)
                        || (p.description() != null
                                && p.description().toLowerCase(Locale.ROOT).contains(normalized)))
                .filter(p -> category == null || category.isBlank() || category.equals(p.category()))
                .filter(p -> shopId == null || shopId == p.shopId())
                .filter(p -> minPrice == null || p.price().compareTo(minPrice) >= 0)
                .filter(p -> maxPrice == null || p.price().compareTo(maxPrice) <= 0)
                .sorted(order)
                .toList();
    }

    public Product publicDetail(long id) {
        return db.sql("select * from products where id=:id and status='ON_SALE'")
                .param("id", id)
                .query(Product.class)
                .optional()
                .orElseThrow(() -> new DomainException("PRODUCT_NOT_FOUND", "在售商品不存在"));
    }

    public record Product(long id, long sellerId, long shopId, String name, String description,
            String category, BigDecimal price, int stock, String status, Instant updatedAt) {}
}

class DomainException extends RuntimeException {
    final String code;

    DomainException(String code, String message) {
        super(message);
        this.code = code;
    }
}
