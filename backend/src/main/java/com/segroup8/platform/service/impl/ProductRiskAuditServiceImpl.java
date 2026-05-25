package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.ProductStatusEnum;
import com.segroup8.platform.common.RoleEnum;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.ProductRiskAuditDecisionRequest;
import com.segroup8.platform.dto.ProductRiskAuditQueryRequest;
import com.segroup8.platform.entity.Product;
import com.segroup8.platform.entity.ProductRiskAudit;
import com.segroup8.platform.entity.SecondhandProduct;
import com.segroup8.platform.entity.Shop;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.ProductMapper;
import com.segroup8.platform.mapper.ProductRiskAuditMapper;
import com.segroup8.platform.mapper.SecondhandProductMapper;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.mapper.UserReportMapper;
import com.segroup8.platform.service.AdminAuditLogService;
import com.segroup8.platform.service.ProductRiskAuditService;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.ProductRiskAuditVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ProductRiskAuditServiceImpl implements ProductRiskAuditService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
    private static final List<String> SENSITIVE_WORDS = List.of("weixin", "qq", "vx", "private chat", "fake receipt",
            "加微信", "私聊", "高仿", "假货", "刷单");
    private static final List<String> PROOF_WORDS = List.of("genuine", "authentic", "original", "正品", "原装", "全新正品");

    private final ProductRiskAuditMapper productRiskAuditMapper;
    private final ProductMapper productMapper;
    private final SecondhandProductMapper secondhandProductMapper;
    private final UserMapper userMapper;
    private final ShopMapper shopMapper;
    private final UserReportMapper userReportMapper;
    private final AdminAuditLogService adminAuditLogService;

    public ProductRiskAuditServiceImpl(ProductRiskAuditMapper productRiskAuditMapper,
            ProductMapper productMapper,
            SecondhandProductMapper secondhandProductMapper,
            UserMapper userMapper,
            ShopMapper shopMapper,
            UserReportMapper userReportMapper,
            AdminAuditLogService adminAuditLogService) {
        this.productRiskAuditMapper = productRiskAuditMapper;
        this.productMapper = productMapper;
        this.secondhandProductMapper = secondhandProductMapper;
        this.userMapper = userMapper;
        this.shopMapper = shopMapper;
        this.userReportMapper = userReportMapper;
        this.adminAuditLogService = adminAuditLogService;
    }

    @Override
    public void auditNewProduct(Product product) {
        if (product == null || product.getId() == null) {
            return;
        }
        Shop shop = product.getShopId() == null ? null : shopMapper.selectById(product.getShopId());
        Long sellerUserId = shop == null ? null : shop.getOwnerUserId();
        User seller = sellerUserId == null ? null : userMapper.selectById(sellerUserId);
        List<String> reasons = evaluateCommon(product.getName(), product.getDescription(), product.getImages(),
                product.getPrice(), null, seller);
        if (product.getStock() == null || product.getStock() <= 0) {
            reasons.add("Stock is zero or missing");
        }
        saveAudit("NEW", product.getId(), sellerUserId, product.getName(), reasons);
    }

    @Override
    public void auditSecondhandProduct(SecondhandProduct product) {
        if (product == null || product.getId() == null) {
            return;
        }
        User seller = product.getSellerUserId() == null ? null : userMapper.selectById(product.getSellerUserId());
        List<String> reasons = evaluateCommon(product.getName(), product.getDescription(), product.getImages(),
                product.getSalePrice(), product.getOriginPrice(), seller);
        if (!StringUtils.hasText(product.getConditionLevel())) {
            reasons.add("Secondhand condition level is missing");
        }
        if (product.getOriginPrice() == null) {
            reasons.add("Original price is missing, price discount cannot be verified");
        } else if (product.getSalePrice() != null
                && product.getSalePrice().compareTo(product.getOriginPrice().multiply(new BigDecimal("0.30"))) < 0) {
            reasons.add("Sale price is far lower than original price");
        }
        saveAudit("SECONDHAND", product.getId(), product.getSellerUserId(), product.getName(), reasons);
    }

    @Override
    public ProductRiskAuditVO getLatestAudit(String productType, Long productId) {
        if (!StringUtils.hasText(productType) || productId == null) {
            return null;
        }
        ProductRiskAudit audit = productRiskAuditMapper.selectOne(new LambdaQueryWrapper<ProductRiskAudit>()
                .eq(ProductRiskAudit::getProductType, productType.trim().toUpperCase())
                .eq(ProductRiskAudit::getProductId, productId)
                .orderByDesc(ProductRiskAudit::getId)
                .last("limit 1"));
        return audit == null ? null : toVO(audit);
    }

    @Override
    public PageVO<ProductRiskAuditVO> pageAudits(ProductRiskAuditQueryRequest request) {
        assertAdmin();
        LambdaQueryWrapper<ProductRiskAudit> wrapper = new LambdaQueryWrapper<ProductRiskAudit>()
                .orderByDesc(ProductRiskAudit::getId);
        if (StringUtils.hasText(request.getProductType())) {
            wrapper.eq(ProductRiskAudit::getProductType, request.getProductType().trim());
        }
        if (StringUtils.hasText(request.getRiskLevel())) {
            wrapper.eq(ProductRiskAudit::getRiskLevel, request.getRiskLevel().trim());
        }
        if (StringUtils.hasText(request.getAuditStatus())) {
            wrapper.eq(ProductRiskAudit::getAuditStatus, request.getAuditStatus().trim());
        }
        if (request.getSellerUserId() != null) {
            wrapper.eq(ProductRiskAudit::getSellerUserId, request.getSellerUserId());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(ProductRiskAudit::getProductName, request.getKeyword().trim());
        }
        Page<ProductRiskAudit> page = productRiskAuditMapper.selectPage(
                Page.of(request.getPageNum(), request.getPageSize()), wrapper);
        PageVO<ProductRiskAuditVO> result = new PageVO<>();
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    @Override
    public ProductRiskAuditVO decide(Long auditId, ProductRiskAuditDecisionRequest request) {
        assertAdmin();
        ProductRiskAudit audit = productRiskAuditMapper.selectById(auditId);
        if (audit == null) {
            throw new BusinessException(404, "Risk audit record not found");
        }
        String decision = normalizeDecision(request.getDecision());
        audit.setAuditStatus(decision);
        audit.setAdminUserId(UserContext.getUserId());
        audit.setAdminRemark(request.getAdminRemark());
        audit.setAuditTime(LocalDateTime.now());
        productRiskAuditMapper.updateById(audit);
        applyProductDecision(audit, decision);
        adminAuditLogService.record("PRODUCT_RISK_AUDIT", audit.getProductType(), audit.getProductId(),
                "decision=" + decision + ", riskLevel=" + audit.getRiskLevel());
        return toVO(productRiskAuditMapper.selectById(auditId));
    }

    private void applyProductDecision(ProductRiskAudit audit, String decision) {
        if (audit == null || Objects.equals(decision, "APPROVED")) {
            return;
        }
        if (Objects.equals(audit.getProductType(), "NEW")) {
            Product product = productMapper.selectById(audit.getProductId());
            if (product != null) {
                product.setStatus(ProductStatusEnum.OFF_SHELF.getCode());
                productMapper.updateById(product);
            }
            return;
        }
        if (Objects.equals(audit.getProductType(), "SECONDHAND")) {
            SecondhandProduct product = secondhandProductMapper.selectById(audit.getProductId());
            if (product != null) {
                product.setStatus(2);
                secondhandProductMapper.updateById(product);
            }
        }
    }

    private List<String> evaluateCommon(String name, String description, String imagesJson,
            BigDecimal price, BigDecimal originPrice, User seller) {
        List<String> reasons = new ArrayList<>();
        String text = ((name == null ? "" : name) + " " + (description == null ? "" : description)).toLowerCase();
        for (String word : SENSITIVE_WORDS) {
            if (text.contains(word.toLowerCase())) {
                reasons.add("Sensitive word detected: " + word);
                break;
            }
        }
        if (!StringUtils.hasText(description) || description.trim().length() < 20) {
            reasons.add("Description is too short");
        }
        if (parseImages(imagesJson).isEmpty()) {
            reasons.add("No product image uploaded");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            reasons.add("Price is missing or invalid");
        }
        if (mentionsProofWord(text) && !text.contains("certificate") && !text.contains("invoice")
                && !text.contains("凭证") && !text.contains("发票")) {
            reasons.add("Title or description claims authenticity but no proof is described");
        }
        if (seller != null) {
            Integer score = firstNonNull(seller.getSellerCreditScore(), seller.getShSellerCreditScore(),
                    seller.getCreditScore());
            if (score != null && score < 60) {
                reasons.add("Seller credit score is low");
            }
            if (seller.getId() != null && userReportMapper.countUpheldReportsIn2Years(seller.getId()) > 0) {
                reasons.add("Seller has upheld reports in recent records");
            }
        }
        if (originPrice != null && price != null && originPrice.compareTo(BigDecimal.ZERO) > 0
                && price.compareTo(originPrice.multiply(new BigDecimal("0.10"))) < 0) {
            reasons.add("Price is extremely lower than original price");
        }
        return reasons;
    }

    private void saveAudit(String productType, Long productId, Long sellerUserId, String productName,
            List<String> reasons) {
        int score = calculateScore(reasons);
        ProductRiskAudit audit = productRiskAuditMapper.selectOne(new LambdaQueryWrapper<ProductRiskAudit>()
                .eq(ProductRiskAudit::getProductType, productType)
                .eq(ProductRiskAudit::getProductId, productId)
                .last("limit 1"));
        if (audit == null) {
            audit = new ProductRiskAudit();
            audit.setProductType(productType);
            audit.setProductId(productId);
        }
        audit.setSellerUserId(sellerUserId);
        audit.setProductName(productName);
        audit.setRiskScore(score);
        audit.setRiskLevel(score >= 70 ? "HIGH" : score >= 35 ? "MEDIUM" : "LOW");
        audit.setSuggestion(score >= 70 ? "ADMIN_REVIEW" : score >= 35 ? "REQUIRE_PROOF" : "AUTO_PASS");
        audit.setAuditStatus("PENDING");
        audit.setRiskReasons(serializeReasons(reasons));
        if (audit.getId() == null) {
            productRiskAuditMapper.insert(audit);
        } else {
            productRiskAuditMapper.updateById(audit);
        }
    }

    private int calculateScore(List<String> reasons) {
        int score = 0;
        for (String reason : reasons) {
            if (reason.contains("Sensitive") || reason.contains("extremely") || reason.contains("upheld reports")) {
                score += 35;
            } else if (reason.contains("credit") || reason.contains("proof") || reason.contains("far lower")) {
                score += 25;
            } else {
                score += 15;
            }
        }
        return Math.min(score, 100);
    }

    private boolean mentionsProofWord(String text) {
        for (String word : PROOF_WORDS) {
            if (text.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private List<String> parseImages(String imagesJson) {
        if (!StringUtils.hasText(imagesJson)) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(imagesJson, STRING_LIST_TYPE);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String serializeReasons(List<String> reasons) {
        try {
            return OBJECT_MAPPER.writeValueAsString(reasons == null ? List.of() : reasons);
        } catch (Exception e) {
            return "[]";
        }
    }

    private ProductRiskAuditVO toVO(ProductRiskAudit audit) {
        ProductRiskAuditVO vo = new ProductRiskAuditVO();
        vo.setId(audit.getId());
        vo.setProductType(audit.getProductType());
        vo.setProductId(audit.getProductId());
        vo.setSellerUserId(audit.getSellerUserId());
        User seller = audit.getSellerUserId() == null ? null : userMapper.selectById(audit.getSellerUserId());
        if (seller != null) {
            vo.setSellerName(StringUtils.hasText(seller.getNickname()) ? seller.getNickname() : seller.getUsername());
        }
        vo.setProductName(audit.getProductName());
        Integer riskScore = audit.getRiskScore() == null ? 0 : audit.getRiskScore();
        String riskLevel = StringUtils.hasText(audit.getRiskLevel())
                ? audit.getRiskLevel()
                : (riskScore >= 70 ? "HIGH" : riskScore >= 35 ? "MEDIUM" : "LOW");
        vo.setRiskLevel(riskLevel);
        vo.setRiskScore(riskScore);
        vo.setRiskReasons(parseReasons(audit.getRiskReasons()));
        vo.setSuggestion(audit.getSuggestion());
        vo.setAuditStatus(audit.getAuditStatus());
        vo.setAdminUserId(audit.getAdminUserId());
        vo.setAdminRemark(audit.getAdminRemark());
        vo.setAuditTime(audit.getAuditTime());
        vo.setCreateTime(audit.getCreateTime());
        return vo;
    }

    private List<String> parseReasons(String reasonsJson) {
        if (!StringUtils.hasText(reasonsJson)) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(reasonsJson, STRING_LIST_TYPE);
        } catch (Exception ignored) {
            return List.of(reasonsJson);
        }
    }

    private Integer firstNonNull(Integer first, Integer second, Integer third) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return third;
    }

    private String normalizeDecision(String decision) {
        String normalized = decision == null ? "" : decision.trim().toUpperCase();
        if (!Objects.equals(normalized, "APPROVED")
                && !Objects.equals(normalized, "REJECTED")
                && !Objects.equals(normalized, "CHANGE_REQUESTED")) {
            throw new BusinessException(400, "Unsupported audit decision");
        }
        return normalized;
    }

    private void assertAdmin() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "Not logged in");
        }
        User user = userMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getRole(), RoleEnum.ADMIN.name())) {
            throw new BusinessException(403, "Admin permission required");
        }
    }
}
