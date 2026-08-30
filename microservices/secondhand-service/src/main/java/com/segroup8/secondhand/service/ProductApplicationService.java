package com.segroup8.secondhand.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.secondhand.api.ProductSaveRequest;
import com.segroup8.secondhand.api.ProductView;
import com.segroup8.secondhand.api.SellerPublicView;
import com.segroup8.secondhand.common.DomainException;
import com.segroup8.secondhand.common.PageResponse;
import com.segroup8.secondhand.domain.SecondhandProduct;
import com.segroup8.secondhand.repository.OutboxRepository;
import com.segroup8.secondhand.repository.IdempotencyRepository;
import com.segroup8.secondhand.repository.ProductRepository;
import com.segroup8.secondhand.repository.ProductRepository.CategoryProjection;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductApplicationService {
    private static final Logger log = LoggerFactory.getLogger(ProductApplicationService.class);
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };
    private final ProductRepository products;
    private final OutboxRepository outbox;
    private final IdempotencyRepository idempotency;
    private final ObjectMapper objectMapper;

    public ProductApplicationService(ProductRepository products, OutboxRepository outbox,
            IdempotencyRepository idempotency, ObjectMapper objectMapper) {
        this.products = products;
        this.outbox = outbox;
        this.idempotency = idempotency;
        this.objectMapper = objectMapper;
    }

    public PageResponse<ProductView> listPublic(long pageNum, long pageSize, String keyword,
            Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, String conditionLevel,
            Integer negotiable, String sortBy) {
        validatePage(pageNum, pageSize);
        validatePriceRange(minPrice, maxPrice);
        var page = products.searchPublic(pageNum, pageSize, keyword, categoryId, minPrice, maxPrice,
                conditionLevel, negotiable, null, sortBy);
        return new PageResponse<>(page.total(), pageNum, pageSize, page.products().stream().map(this::toView).toList());
    }

    public PageResponse<ProductView> listPublicSeller(long sellerId, long pageNum, long pageSize,
            String keyword, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice,
            String conditionLevel, Integer negotiable, String sortBy) {
        validatePage(pageNum, pageSize);
        var page = products.searchPublic(pageNum, pageSize, keyword, categoryId, minPrice, maxPrice,
                conditionLevel, negotiable, sellerId, sortBy);
        return new PageResponse<>(page.total(), pageNum, pageSize, page.products().stream().map(this::toView).toList());
    }

    public ProductView publicDetail(long productId) {
        return toView(products.findPublicById(productId)
                .orElseThrow(() -> DomainException.notFound("PRODUCT_NOT_FOUND", "二手商品不存在或不可见")));
    }

    public PageResponse<ProductView> listSeller(long sellerId, long pageNum, long pageSize, String keyword,
            Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, String conditionLevel,
            Integer negotiable, Integer status, String sortBy) {
        validatePage(pageNum, pageSize);
        validatePriceRange(minPrice, maxPrice);
        var page = products.searchSeller(sellerId, pageNum, pageSize, keyword, categoryId, minPrice,
                maxPrice, conditionLevel, negotiable, status, sortBy);
        return new PageResponse<>(page.total(), pageNum, pageSize, page.products().stream().map(this::toView).toList());
    }

    @Transactional
    public ProductView create(long sellerId, String sellerName, ProductSaveRequest request) {
        validateSave(request);
        int requestedStatus = request.status() == null ? SecondhandProduct.ON_SHELF : request.status();
        List<String> images = normalizedImages(request.images());
        long id = products.insert(sellerId, sellerName, request.name().trim(), images.get(0), json(images),
                request.description(), request.originPrice(), request.salePrice(), request.categoryId(),
                request.subCategoryId(), request.conditionLevel().trim(), request.isNegotiable(), requestedStatus);
        appendProductSubmitted(id, sellerId, request);
        return toView(requireProduct(id));
    }

    @Transactional
    public ProductView update(long sellerId, long productId, ProductSaveRequest request) {
        validateSave(request);
        SecondhandProduct current = requireOwnedMutable(sellerId, productId);
        List<String> images = normalizedImages(request.images());
        int requestedStatus = request.status() == null ? current.status() : request.status();
        int changed = products.updateOwned(current, request.name().trim(), images.get(0), json(images),
                request.description(), request.originPrice(), request.salePrice(), request.categoryId(),
                request.subCategoryId(), request.conditionLevel().trim(), request.isNegotiable(), requestedStatus);
        if (changed == 0) {
            throw DomainException.conflict("PRODUCT_CONFLICT", "商品已被其他操作修改，请刷新后重试");
        }
        appendProductSubmitted(productId, sellerId, request);
        return toView(requireProduct(productId));
    }

    @Transactional
    public void delete(long sellerId, long productId) {
        SecondhandProduct product = requireOwnedMutable(sellerId, productId);
        if (products.softDelete(productId, sellerId, product.version()) == 0) {
            throw DomainException.conflict("PRODUCT_CONFLICT", "商品状态已变化，无法删除");
        }
        outbox.append("SECONDHAND_PRODUCT", productId, "SecondhandProductDeleted.v1",
                Map.of("productId", productId, "sellerUserId", sellerId));
    }

    @Transactional
    public ProductView changeStatus(long sellerId, long productId, int status) {
        SecondhandProduct product = requireOwnedMutable(sellerId, productId);
        if (status == SecondhandProduct.ON_SHELF && !"APPROVED".equals(product.riskStatus())) {
            throw DomainException.conflict("PRODUCT_RISK_PENDING", "商品仍在审核中，暂不能上架");
        }
        if (products.changeStatus(productId, sellerId, product.version(), status) == 0) {
            throw DomainException.conflict("PRODUCT_CONFLICT", "商品状态已变化，请刷新后重试");
        }
        return toView(requireProduct(productId));
    }

    @Transactional
    public boolean applyRiskDecision(String eventId, long productId, String decision) {
        String normalized = decision == null ? "" : decision.toUpperCase();
        if (!List.of("APPROVED", "REJECTED", "RISK_PENDING").contains(normalized)) {
            throw DomainException.badRequest("RISK_DECISION_INVALID", "风险审核结果不合法");
        }
        if (!idempotency.recordOnce("RISK_DECISION_EVENT", eventId, String.valueOf(productId))) {
            log.info("risk decision event duplicate eventId={} productId={} decision={}",
                    eventId, productId, normalized);
            return false;
        }
        if (products.markRiskDecision(productId, normalized) == 0) {
            throw DomainException.notFound("PRODUCT_NOT_FOUND", "二手商品不存在");
        }
        outbox.append("SECONDHAND_PRODUCT", productId, "SecondhandRiskDecisionApplied.v1",
                Map.of("productId", productId, "decision", normalized));
        log.info("risk decision event consumed eventId={} productId={} decision={}",
                eventId, productId, normalized);
        return true;
    }

    public SellerPublicView sellerPublic(long sellerId) {
        var page = products.searchPublic(1, 1, null, null, null, null, null, null, sellerId, null);
        if (page.total() == 0) {
            throw DomainException.notFound("SELLER_NOT_FOUND", "卖家暂无公开二手商品");
        }
        String name = page.products().get(0).sellerNameSnapshot();
        long completed = products.countCompletedForSeller(sellerId);
        return new SellerPublicView(sellerId, name == null || name.isBlank() ? "用户" + sellerId : name,
                null, null, new SellerPublicView.SellerRatingView(100, "信用良好", completed));
    }

    public ProductView toView(SecondhandProduct product) {
        CategoryProjection category = products.findCategory(product.categoryId(), product.subCategoryId()).orElse(null);
        String sellerName = product.sellerNameSnapshot() == null || product.sellerNameSnapshot().isBlank()
                ? "用户" + product.sellerUserId() : product.sellerNameSnapshot();
        return new ProductView(product.id(), product.sellerUserId(), sellerName, product.name(), product.cover(),
                readImages(product.imagesJson()), product.description(), product.originPrice(), product.salePrice(),
                product.categoryId(), product.subCategoryId(), category == null ? null : category.categoryName(),
                category == null ? null : category.subCategoryName(), product.conditionLevel(), product.negotiable(),
                product.status(), statusName(product.status()),
                new ProductView.RiskView(product.riskStatus(), "APPROVED".equals(product.riskStatus())
                        ? "AUTO_PASS" : "MANUAL_REVIEW"), product.createTime());
    }

    private void appendProductSubmitted(long productId, long sellerId, ProductSaveRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productType", "SECONDHAND");
        payload.put("productId", productId);
        payload.put("sellerUserId", sellerId);
        payload.put("name", request.name().trim());
        payload.put("description", request.description() == null ? "" : request.description());
        payload.put("categoryId", request.categoryId());
        payload.put("subCategoryId", request.subCategoryId());
        payload.put("price", request.salePrice());
        outbox.append("SECONDHAND_PRODUCT", productId, "ProductSubmitted.v1", payload);
    }

    private void validateSave(ProductSaveRequest request) {
        if (request.salePrice().compareTo(request.originPrice()) > 0) {
            throw DomainException.badRequest("PRICE_INVALID", "二手售价不能高于原价");
        }
        if (products.findCategory(request.categoryId(), request.subCategoryId()).isEmpty()) {
            throw DomainException.badRequest("CATEGORY_INVALID", "所选二手分类不存在或已停用");
        }
    }

    private SecondhandProduct requireOwnedMutable(long sellerId, long productId) {
        SecondhandProduct product = requireProduct(productId);
        if (!product.ownedBy(sellerId)) {
            throw DomainException.forbidden("OWNERSHIP_REQUIRED", "只能管理自己发布的二手商品");
        }
        if (product.status() == SecondhandProduct.SOLD || product.status() == SecondhandProduct.TRADE_PENDING) {
            throw DomainException.conflict("PRODUCT_STATE_CONFLICT", "成交中或已售商品不能修改");
        }
        return product;
    }

    private SecondhandProduct requireProduct(long productId) {
        return products.findById(productId)
                .orElseThrow(() -> DomainException.notFound("PRODUCT_NOT_FOUND", "二手商品不存在"));
    }

    private void validatePage(long pageNum, long pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw DomainException.badRequest("PAGE_INVALID", "页码必须大于0且每页最多100条");
        }
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw DomainException.badRequest("PRICE_RANGE_INVALID", "最低价不能高于最高价");
        }
    }

    private List<String> normalizedImages(List<String> images) {
        return images.stream().map(String::trim).filter(value -> !value.isBlank()).distinct().toList();
    }

    private String json(List<String> images) {
        try {
            return objectMapper.writeValueAsString(images);
        } catch (JsonProcessingException exception) {
            throw DomainException.badRequest("IMAGE_INVALID", "商品图片格式不正确");
        }
    }

    private List<String> readImages(String json) {
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private String statusName(int status) {
        return switch (status) {
            case SecondhandProduct.ON_SHELF -> "在售";
            case SecondhandProduct.OFF_SHELF -> "已下架";
            case SecondhandProduct.SOLD -> "已售出";
            case SecondhandProduct.TRADE_PENDING -> "成交处理中";
            default -> "未知";
        };
    }
}
