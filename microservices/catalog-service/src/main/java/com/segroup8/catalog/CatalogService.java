package com.segroup8.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CatalogService {
    private final JdbcClient db;
    private final SimpleJdbcInsert productInsert;
    private final RestClient riskClient;

    public CatalogService(JdbcClient db, DataSource dataSource, RestClient.Builder builder,
            @Value("${clients.risk-base-url:http://risk-service:8083}") String riskBaseUrl) {
        this.db = db;
        this.productInsert = new SimpleJdbcInsert(dataSource).withTableName("products").usingGeneratedKeyColumns("id");
        this.riskClient = builder.baseUrl(riskBaseUrl).build();
    }

    public List<Product> search(String keyword, String category, Long shopId, BigDecimal minPrice, BigDecimal maxPrice, String sort) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        Comparator<Product> order = switch (sort == null ? "newest" : sort) {
            case "priceAsc" -> Comparator.comparing(Product::price);
            case "priceDesc" -> Comparator.comparing(Product::price).reversed();
            default -> Comparator.comparing(Product::updatedAt).reversed();
        };
        return db.sql("select * from products where status='ON_SALE'").query(Product.class).list().stream()
                .filter(p -> normalized.isBlank() || p.name().toLowerCase(Locale.ROOT).contains(normalized)
                        || (p.description() != null && p.description().toLowerCase(Locale.ROOT).contains(normalized)))
                .filter(p -> category == null || category.isBlank() || category.equals(p.category()))
                .filter(p -> shopId == null || shopId == p.shopId())
                .filter(p -> minPrice == null || p.price().compareTo(minPrice) >= 0)
                .filter(p -> maxPrice == null || p.price().compareTo(maxPrice) <= 0)
                .sorted(order).toList();
    }

    public Product publicDetail(long id) {
        return db.sql("select * from products where id=:id and status='ON_SALE'").param("id", id)
                .query(Product.class).optional().orElseThrow(() -> new DomainException("PRODUCT_NOT_FOUND", "在售商品不存在"));
    }

    public List<Product> sellerProducts(long sellerId) {
        return db.sql("select * from products where seller_id=:seller order by updated_at desc").param("seller", sellerId)
                .query(Product.class).list();
    }

    public Product create(long sellerId, ProductCommand c) {
        validate(c);
        long id = productInsert.executeAndReturnKey(Map.of("seller_id", sellerId, "shop_id", c.shopId(), "name", c.name(),
                "description", nullToEmpty(c.description()), "category", c.category(), "price", c.price(),
                "stock", c.stock(), "status", "DRAFT", "updated_at", java.sql.Timestamp.from(Instant.now()))).longValue();
        return owned(id, sellerId);
    }

    public Product update(long sellerId, long id, ProductCommand c) {
        validate(c);
        Product current = owned(id, sellerId);
        if (!Set.of("DRAFT", "REJECTED", "OFF_SHELF").contains(current.status()))
            throw new DomainException("INVALID_PRODUCT_STATE", "只有草稿、驳回或下架商品可编辑");
        db.sql("update products set name=:name,description=:description,category=:category,price=:price,stock=:stock,updated_at=CURRENT_TIMESTAMP where id=:id")
                .params(Map.of("name", c.name(), "description", nullToEmpty(c.description()), "category", c.category(),
                        "price", c.price(), "stock", c.stock(), "id", id)).update();
        return owned(id, sellerId);
    }

    public Product transition(long sellerId, long id, String action) {
        Product current = owned(id, sellerId);
        String next = LifecyclePolicy.next(current.status(), action);
        db.sql("update products set status=:status,updated_at=CURRENT_TIMESTAMP where id=:id")
                .params(Map.of("status", next, "id", id)).update();
        if ("SUBMIT".equals(action)) requestRiskAudit(current);
        return owned(id, sellerId);
    }

    public Product applyRiskDecision(long id, boolean approved) {
        Product current = byId(id);
        if (!"PENDING_REVIEW".equals(current.status()))
            throw new DomainException("INVALID_PRODUCT_STATE", "商品不在待审核状态");
        db.sql("update products set status=:status,updated_at=CURRENT_TIMESTAMP where id=:id")
                .params(Map.of("status", approved ? "ON_SALE" : "REJECTED", "id", id)).update();
        return byId(id);
    }

    private void requestRiskAudit(Product p) {
        try {
            riskClient.post().uri("/internal/risk-audits").contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("productId", p.id(), "name", p.name(), "description", nullToEmpty(p.description())))
                    .retrieve().toBodilessEntity();
        } catch (RuntimeException ex) {
            db.sql("insert into integration_outbox(event_type,aggregate_id,payload,status,created_at) "
                            + "values('PRODUCT_RISK_REVIEW_REQUESTED',:id,:payload,'PENDING',CURRENT_TIMESTAMP)")
                    .params(Map.of("id", p.id(), "payload", "{\"productId\":" + p.id() + "}" )).update();
        }
    }

    private Product owned(long id, long sellerId) {
        return db.sql("select * from products where id=:id and seller_id=:seller").params(Map.of("id", id, "seller", sellerId))
                .query(Product.class).optional().orElseThrow(() -> new DomainException("PRODUCT_NOT_FOUND", "商品不存在或不属于当前卖家"));
    }
    private Product byId(long id) {
        return db.sql("select * from products where id=:id").param("id", id).query(Product.class).optional()
                .orElseThrow(() -> new DomainException("PRODUCT_NOT_FOUND", "商品不存在"));
    }
    private void validate(ProductCommand c) {
        if (c.price().signum() <= 0) throw new DomainException("INVALID_PRICE", "价格必须大于 0");
        if (c.stock() < 0) throw new DomainException("INVALID_STOCK", "库存不能小于 0");
    }
    private String nullToEmpty(String value) { return value == null ? "" : value; }

    public record Product(long id, long sellerId, long shopId, String name, String description, String category,
                          BigDecimal price, int stock, String status, Instant updatedAt) {}
    public record ProductCommand(long shopId, String name, String description, String category, BigDecimal price, int stock) {}
}

final class LifecyclePolicy {
    private static final Map<String, Map<String, String>> RULES = Map.of(
            "DRAFT", Map.of("SUBMIT", "PENDING_REVIEW", "ARCHIVE", "ARCHIVED"),
            "REJECTED", Map.of("SUBMIT", "PENDING_REVIEW", "ARCHIVE", "ARCHIVED"),
            "ON_SALE", Map.of("OFF_SHELF", "OFF_SHELF"),
            "OFF_SHELF", Map.of("SUBMIT", "PENDING_REVIEW", "ARCHIVE", "ARCHIVED"));
    static String next(String current, String action) {
        String next = RULES.getOrDefault(current, Map.of()).get(action);
        if (next == null) throw new DomainException("INVALID_PRODUCT_TRANSITION", current + " 不能执行 " + action);
        return next;
    }
    private LifecyclePolicy() {}
}

class DomainException extends RuntimeException {
    final String code;
    DomainException(String code, String message) { super(message); this.code = code; }
}
