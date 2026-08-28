package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.ProductStatusEnum;
import com.segroup8.platform.common.RoleEnum;
import com.segroup8.platform.config.UploadProperties;
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
import com.segroup8.platform.service.NotificationService;
import com.segroup8.platform.service.ProductRiskAuditService;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.ProductRiskAuditVO;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ProductRiskAuditServiceImpl implements ProductRiskAuditService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
    private static final List<String> SENSITIVE_WORDS = List.of("weixin", "qq", "vx", "private chat", "fake receipt",
            "加微信", "私聊", "高仿", "假货", "刷单");
    private static final List<String> PROOF_WORDS = List.of("genuine", "authentic", "original", "正品", "原装", "全新正品");

    private static final String DEFAULT_LLM_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_LLM_RESPONSES_ENDPOINT = "https://api.openai.com/v1/responses";
    private static final String DEFAULT_LLM_MODEL = "gpt-4o-mini";
    private static final int DEFAULT_LLM_TIMEOUT_SECONDS = 12;
    private static final int MAX_LLM_IMAGE_COUNT = 4;
    private static final long MAX_LLM_IMAGE_BYTES = 5L * 1024 * 1024;

    private final ProductRiskAuditMapper productRiskAuditMapper;
    private final ProductMapper productMapper;
    private final SecondhandProductMapper secondhandProductMapper;
    private final UserMapper userMapper;
    private final ShopMapper shopMapper;
    private final UserReportMapper userReportMapper;
    private final AdminAuditLogService adminAuditLogService;
    private final NotificationService notificationService;
    private final UploadProperties uploadProperties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(DEFAULT_LLM_TIMEOUT_SECONDS))
            .build();
    private final ExecutorService largeModelAuditExecutor = Executors.newFixedThreadPool(2);

    @Value("${app.risk-audit.llm.enabled:true}")
    private boolean riskAuditLlmEnabled;

    @Value("${app.risk-audit.llm.api-key:}")
    private String riskAuditLlmApiKey;

    @Value("${app.risk-audit.llm.model:gpt-4o-mini}")
    private String riskAuditLlmModel;

    @Value("${app.risk-audit.llm.endpoint:}")
    private String riskAuditLlmEndpoint;

    @Value("${app.risk-audit.llm.base-url:}")
    private String riskAuditLlmBaseUrl;

    @Value("${app.risk-audit.llm.wire-api:responses}")
    private String riskAuditLlmWireApi;

    @Value("${app.risk-audit.llm.disable-response-storage:true}")
    private boolean riskAuditDisableResponseStorage;

    @Value("${app.risk-audit.llm.timeout-seconds:12}")
    private int riskAuditLlmTimeoutSeconds;

    public ProductRiskAuditServiceImpl(ProductRiskAuditMapper productRiskAuditMapper,
            ProductMapper productMapper,
            SecondhandProductMapper secondhandProductMapper,
            UserMapper userMapper,
            ShopMapper shopMapper,
            UserReportMapper userReportMapper,
            AdminAuditLogService adminAuditLogService,
            NotificationService notificationService,
            UploadProperties uploadProperties) {
        this.productRiskAuditMapper = productRiskAuditMapper;
        this.productMapper = productMapper;
        this.secondhandProductMapper = secondhandProductMapper;
        this.userMapper = userMapper;
        this.shopMapper = shopMapper;
        this.userReportMapper = userReportMapper;
        this.adminAuditLogService = adminAuditLogService;
        this.notificationService = notificationService;
        this.uploadProperties = uploadProperties;
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
        RiskEvaluation fallback = buildRuleEvaluation(reasons);
        List<String> imageUrls = parseImages(product.getImages());
        ProductRiskAuditPromptBuilder.AuditContext payload = new ProductRiskAuditPromptBuilder.AuditContext(
                "NEW", product.getId(), product.getName(),
                product.getDescription(), product.getPrice(), null, null, product.getStock(),
                imageUrls, buildModelImageInputs(imageUrls), shop == null ? null : shop.getId(),
                shop == null ? null : shop.getName(), shop == null ? null : shop.getRegion(),
                shop == null ? null : shop.getStatus(), sellerUserId, getSellerCreditScore(seller),
                countUpheldReports(seller), reasons);
        RiskEvaluation evaluation = evaluateWithLargeModelIfConfigured(payload, fallback);
        saveAudit("NEW", product.getId(), sellerUserId, product.getName(), evaluation);
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
        RiskEvaluation fallback = buildRuleEvaluation(reasons);
        List<String> imageUrls = parseImages(product.getImages());
        ProductRiskAuditPromptBuilder.AuditContext payload = new ProductRiskAuditPromptBuilder.AuditContext(
                "SECONDHAND", product.getId(), product.getName(),
                product.getDescription(), product.getSalePrice(), product.getOriginPrice(),
                product.getConditionLevel(), null, imageUrls, buildModelImageInputs(imageUrls), null, null, null, null,
                product.getSellerUserId(), getSellerCreditScore(seller), countUpheldReports(seller), reasons);
        RiskEvaluation evaluation = evaluateWithLargeModelIfConfigured(payload, fallback);
        saveAudit("SECONDHAND", product.getId(), product.getSellerUserId(), product.getName(), evaluation);
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
        notifySellerIfProductAuditBlocked(audit, decision, request.getAdminRemark());
        adminAuditLogService.record("PRODUCT_RISK_AUDIT", audit.getProductType(), audit.getProductId(),
                "decision=" + decision + ", riskLevel=" + audit.getRiskLevel());
        return toVO(productRiskAuditMapper.selectById(auditId));
    }

    private void applyProductDecision(ProductRiskAudit audit, String decision) {
        if (audit == null) {
            return;
        }
        boolean approved = Objects.equals(decision, "APPROVED");
        if (Objects.equals(audit.getProductType(), "NEW")) {
            Product product = productMapper.selectById(audit.getProductId());
            if (product != null) {
                product.setStatus(approved ? ProductStatusEnum.ON_SHELF.getCode() : ProductStatusEnum.OFF_SHELF.getCode());
                productMapper.updateById(product);
            }
            return;
        }
        if (Objects.equals(audit.getProductType(), "SECONDHAND")) {
            SecondhandProduct product = secondhandProductMapper.selectById(audit.getProductId());
            if (product != null) {
                product.setStatus(approved ? 1 : 2);
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
            if (seller.getId() != null && countUpheldReportsIn2Years(seller.getId()) > 0) {
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
            RiskEvaluation evaluation) {
        saveAudit(productType, productId, sellerUserId, productName, evaluation, true);
    }

    private void saveAudit(String productType, Long productId, Long sellerUserId, String productName,
            RiskEvaluation evaluation, boolean resetAuditStatus) {
        ProductRiskAudit audit = productRiskAuditMapper.selectOne(new LambdaQueryWrapper<ProductRiskAudit>()
                .eq(ProductRiskAudit::getProductType, productType)
                .eq(ProductRiskAudit::getProductId, productId)
                .last("limit 1"));
        boolean exists = audit != null;
        if (!resetAuditStatus && exists && audit.getAdminUserId() != null) {
            return;
        }
        if (audit == null) {
            audit = new ProductRiskAudit();
            audit.setProductType(productType);
            audit.setProductId(productId);
        }
        audit.setSellerUserId(sellerUserId);
        audit.setProductName(productName);
        audit.setRiskScore(evaluation.riskScore());
        audit.setRiskLevel(evaluation.riskLevel());
        audit.setSuggestion(evaluation.suggestion());
        if (resetAuditStatus || !exists || !StringUtils.hasText(audit.getAuditStatus())
                || audit.getAdminUserId() == null) {
            audit.setAuditStatus(isAutoApprovedRisk(evaluation.riskLevel()) ? "APPROVED" : "PENDING");
            audit.setAdminUserId(null);
            audit.setAdminRemark(null);
            audit.setAuditTime(null);
        }
        audit.setRiskReasons(serializeReasons(evaluation.riskReasons()));
        if (audit.getId() == null) {
            productRiskAuditMapper.insert(audit);
        } else {
            productRiskAuditMapper.updateById(audit);
        }
        applyProductDecision(audit, audit.getAuditStatus());
    }

    private void submitLargeModelEvaluation(String productType, Long productId, Long sellerUserId, String productName,
            ProductRiskAuditPromptBuilder.AuditContext payload, RiskEvaluation fallback) {
        if (!isLargeModelConfigured()) {
            return;
        }
        largeModelAuditExecutor.submit(() -> {
            RiskEvaluation evaluation = evaluateByLargeModel(payload, fallback);
            saveAudit(productType, productId, sellerUserId, productName, evaluation, false);
        });
    }

    private RiskEvaluation evaluateWithLargeModelIfConfigured(ProductRiskAuditPromptBuilder.AuditContext payload,
            RiskEvaluation fallback) {
        if (!isLargeModelConfigured()) {
            return fallback;
        }
        return evaluateByLargeModel(payload, fallback);
    }

    private RiskEvaluation buildRuleEvaluation(List<String> reasons) {
        int score = calculateScore(reasons);
        return new RiskEvaluation(score, defaultRiskLevel(score), defaultSuggestion(score),
                localizeRuleReasons(reasons));
    }

    private RiskEvaluation evaluateByLargeModel(ProductRiskAuditPromptBuilder.AuditContext payload,
            RiskEvaluation fallback) {
        String apiKey = getLargeModelApiKey();
        if (!isLargeModelEnabled(apiKey)) {
            return fallback;
        }
        try {
            String model = firstTextOrDefault(DEFAULT_LLM_MODEL, riskAuditLlmModel,
                    System.getenv("RISK_AUDIT_LLM_MODEL"), System.getenv("OPENAI_MODEL"));
            ObjectNode body = buildLargeModelRequestBody(model, payload, fallback);

            HttpRequest request = HttpRequest.newBuilder(URI.create(getLargeModelEndpoint()))
                    .timeout(Duration.ofSeconds(getLargeModelTimeoutSeconds()))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return fallback;
            }
            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            String content = extractLargeModelContent(root);
            String json = extractJsonObject(content);
            if (!StringUtils.hasText(json)) {
                return fallback;
            }
            return parseLargeModelEvaluation(json, fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private boolean isLargeModelConfigured() {
        return isLargeModelEnabled(getLargeModelApiKey());
    }

    private String getLargeModelApiKey() {
        return firstText(riskAuditLlmApiKey, System.getenv("RISK_AUDIT_LLM_API_KEY"), System.getenv("OPENAI_API_KEY"));
    }

    private ObjectNode buildLargeModelRequestBody(String model, ProductRiskAuditPromptBuilder.AuditContext payload,
            RiskEvaluation fallback) {
        if (isResponsesWireApi()) {
            return buildResponsesRequestBody(model, payload, fallback);
        }
        return buildChatCompletionsRequestBody(model, payload, fallback);
    }

    private ObjectNode buildResponsesRequestBody(String model, ProductRiskAuditPromptBuilder.AuditContext payload,
            RiskEvaluation fallback) {
        ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("model", model);
        body.put("instructions", ProductRiskAuditPromptBuilder.systemPrompt());
        ArrayNode input = body.putArray("input");
        ObjectNode message = input.addObject();
        message.put("role", "user");
        message.set("content", ProductRiskAuditPromptBuilder.responsesContent(OBJECT_MAPPER, payload,
                toRuleBaseline(fallback)));
        body.put("store", !riskAuditDisableResponseStorage);
        addRiskAuditJsonSchema(body.putObject("text").putObject("format"));
        return body;
    }

    private ObjectNode buildChatCompletionsRequestBody(String model, ProductRiskAuditPromptBuilder.AuditContext payload,
            RiskEvaluation fallback) {
        ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("model", model);
        body.putObject("response_format").put("type", "json_object");
        ArrayNode messages = body.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", ProductRiskAuditPromptBuilder.systemPrompt());
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.set("content", ProductRiskAuditPromptBuilder.chatContent(OBJECT_MAPPER, payload, toRuleBaseline(fallback)));
        return body;
    }

    private void addRiskAuditJsonSchema(ObjectNode format) {
        format.put("type", "json_schema");
        format.put("name", "product_risk_audit");
        format.put("strict", true);
        ObjectNode schema = format.putObject("schema");
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("riskLevel")
                .put("type", "string")
                .putArray("enum")
                .add("LOW")
                .add("MEDIUM")
                .add("HIGH");
        ObjectNode score = properties.putObject("riskScore");
        score.put("type", "integer");
        score.put("minimum", 0);
        score.put("maximum", 100);
        ObjectNode reasons = properties.putObject("riskReasons");
        reasons.put("type", "array");
        reasons.putObject("items").put("type", "string");
        properties.putObject("suggestion")
                .put("type", "string")
                .putArray("enum")
                .add("AUTO_PASS")
                .add("REQUIRE_PROOF")
                .add("ADMIN_REVIEW");
        schema.putArray("required")
                .add("riskLevel")
                .add("riskScore")
                .add("riskReasons")
                .add("suggestion");
    }

    private String extractLargeModelContent(JsonNode root) {
        String responsesContent = extractResponsesContent(root);
        if (StringUtils.hasText(responsesContent)) {
            return responsesContent;
        }
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        return contentNode.isMissingNode() ? "" : contentNode.toString();
    }

    private String extractResponsesContent(JsonNode root) {
        JsonNode outputText = root.path("output_text");
        if (outputText.isTextual() && StringUtils.hasText(outputText.asText())) {
            return outputText.asText();
        }
        JsonNode output = root.path("output");
        if (!output.isArray()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode item : output) {
            JsonNode content = item.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode part : content) {
                JsonNode partText = part.path("text");
                if (partText.isTextual() && StringUtils.hasText(partText.asText())) {
                    text.append(partText.asText());
                }
            }
        }
        return text.toString();
    }

    private RiskEvaluation parseLargeModelEvaluation(String json, RiskEvaluation fallback) throws Exception {
        JsonNode node = OBJECT_MAPPER.readTree(json);
        int score = clampScore(node.path("riskScore").asInt(fallback.riskScore()));
        String riskLevel = normalizeRiskLevel(node.path("riskLevel").asText(null), score);
        String suggestion = normalizeSuggestionForRisk(riskLevel, normalizeSuggestion(node.path("suggestion").asText(null), score));
        List<String> reasons = parseLargeModelReasons(node.path("riskReasons"));
        if (reasons.isEmpty()) {
            reasons = parseLargeModelReasons(node.path("riskReason"));
        }
        if (reasons.isEmpty()) {
            reasons = fallback.riskReasons();
        }
        return new RiskEvaluation(score, riskLevel, suggestion, reasons);
    }

    private List<String> parseLargeModelReasons(JsonNode reasonsNode) {
        if (reasonsNode == null || reasonsNode.isMissingNode() || reasonsNode.isNull()) {
            return List.of();
        }
        List<String> reasons = new ArrayList<>();
        if (reasonsNode.isArray()) {
            for (JsonNode item : reasonsNode) {
                if (StringUtils.hasText(item.asText())) {
                    reasons.add(item.asText().trim());
                }
            }
        } else if (StringUtils.hasText(reasonsNode.asText())) {
            reasons.add(reasonsNode.asText().trim());
        }
        return reasons.stream().filter(StringUtils::hasText).distinct().limit(8).toList();
    }

    private String extractJsonObject(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end < start) {
            return "";
        }
        return content.substring(start, end + 1);
    }

    private boolean isLargeModelEnabled(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return false;
        }
        String enabled = System.getenv("RISK_AUDIT_LLM_ENABLED");
        if (!StringUtils.hasText(enabled)) {
            return riskAuditLlmEnabled;
        }
        String normalized = enabled.trim().toLowerCase();
        return !Objects.equals(normalized, "false")
                && !Objects.equals(normalized, "0")
                && !Objects.equals(normalized, "no")
                && !Objects.equals(normalized, "off");
    }

    private String getLargeModelEndpoint() {
        String endpoint = firstText(riskAuditLlmEndpoint, System.getenv("RISK_AUDIT_LLM_ENDPOINT"));
        if (StringUtils.hasText(endpoint)) {
            return endpoint;
        }
        String baseUrl = firstText(riskAuditLlmBaseUrl, System.getenv("RISK_AUDIT_LLM_BASE_URL"),
                System.getenv("OPENAI_BASE_URL"));
        if (!StringUtils.hasText(baseUrl)) {
            return isResponsesWireApi() ? DEFAULT_LLM_RESPONSES_ENDPOINT : DEFAULT_LLM_ENDPOINT;
        }
        String normalized = baseUrl.trim();
        if (normalized.endsWith("/chat/completions") || normalized.endsWith("/responses")) {
            return normalized;
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/v1")) {
            return normalized + (isResponsesWireApi() ? "/responses" : "/chat/completions");
        }
        return normalized + (isResponsesWireApi() ? "/v1/responses" : "/v1/chat/completions");
    }

    private boolean isResponsesWireApi() {
        String wireApi = firstText(riskAuditLlmWireApi, System.getenv("RISK_AUDIT_LLM_WIRE_API"));
        if (!StringUtils.hasText(wireApi)) {
            return true;
        }
        String normalized = wireApi.trim().toLowerCase().replace("_", "-");
        return Objects.equals(normalized, "responses") || Objects.equals(normalized, "response");
    }

    private int getLargeModelTimeoutSeconds() {
        String value = System.getenv("RISK_AUDIT_LLM_TIMEOUT_SECONDS");
        try {
            if (StringUtils.hasText(value)) {
                return Math.max(3, Math.min(30, Integer.parseInt(value.trim())));
            }
            return Math.max(3, Math.min(30, riskAuditLlmTimeoutSeconds));
        } catch (NumberFormatException ignored) {
            return DEFAULT_LLM_TIMEOUT_SECONDS;
        }
    }

    private String firstTextOrDefault(String defaultValue, String... values) {
        String value = firstText(values);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private String normalizeRiskLevel(String riskLevel, int score) {
        if (StringUtils.hasText(riskLevel)) {
            String normalized = riskLevel.trim().toUpperCase();
            if (Objects.equals(normalized, "LOW") || normalized.contains("低")) {
                return "LOW";
            }
            if (Objects.equals(normalized, "MEDIUM") || normalized.contains("中")) {
                return "MEDIUM";
            }
            if (Objects.equals(normalized, "HIGH") || normalized.contains("高")) {
                return "HIGH";
            }
        }
        return defaultRiskLevel(score);
    }

    private String normalizeSuggestion(String suggestion, int score) {
        if (StringUtils.hasText(suggestion)) {
            String normalized = suggestion.trim().toUpperCase();
            if (Objects.equals(normalized, "AUTO_PASS")) {
                return "AUTO_PASS";
            }
            if (Objects.equals(normalized, "REQUIRE_PROOF")) {
                return "REQUIRE_PROOF";
            }
            if (Objects.equals(normalized, "ADMIN_REVIEW")) {
                return "ADMIN_REVIEW";
            }
            if (suggestion.contains("凭证") || suggestion.contains("证明") || suggestion.contains("补充")) {
                return "REQUIRE_PROOF";
            }
            if (suggestion.contains("复核") || suggestion.contains("人工") || suggestion.contains("审核")) {
                return "ADMIN_REVIEW";
            }
            if (suggestion.contains("通过")) {
                return "AUTO_PASS";
            }
        }
        return defaultSuggestion(score);
    }

    private String normalizeSuggestionForRisk(String riskLevel, String suggestion) {
        if (Objects.equals("LOW", riskLevel)) {
            return StringUtils.hasText(suggestion) ? suggestion : "AUTO_PASS";
        }
        if (Objects.equals("HIGH", riskLevel)) {
            return "ADMIN_REVIEW";
        }
        if (Objects.equals("AUTO_PASS", suggestion)) {
            return "REQUIRE_PROOF";
        }
        return StringUtils.hasText(suggestion) ? suggestion : "REQUIRE_PROOF";
    }

    private String defaultRiskLevel(int score) {
        return score >= 70 ? "HIGH" : score >= 35 ? "MEDIUM" : "LOW";
    }

    private String defaultSuggestion(int score) {
        return score >= 70 ? "ADMIN_REVIEW" : score >= 35 ? "REQUIRE_PROOF" : "AUTO_PASS";
    }

    private boolean isAutoApprovedRisk(String riskLevel) {
        return Objects.equals("LOW", riskLevel);
    }

    private ProductRiskAuditPromptBuilder.RuleBaseline toRuleBaseline(RiskEvaluation evaluation) {
        return new ProductRiskAuditPromptBuilder.RuleBaseline(evaluation.riskScore(), evaluation.riskLevel(),
                evaluation.suggestion());
    }

    private Integer getSellerCreditScore(User seller) {
        return seller == null ? null : firstNonNull(seller.getSellerCreditScore(), seller.getShSellerCreditScore(),
                seller.getCreditScore());
    }

    private long countUpheldReports(User seller) {
        if (seller == null || seller.getId() == null) {
            return 0;
        }
        return countUpheldReportsIn2Years(seller.getId());
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

    private List<String> localizeRuleReasons(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return List.of("规则初筛未发现明显风险");
        }
        return reasons.stream().map(this::localizeRuleReason).distinct().toList();
    }

    private String localizeRuleReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return "规则初筛发现未命名风险";
        }
        if (reason.startsWith("Sensitive word detected:")) {
            return "商品标题或描述包含敏感词/引流词：" + reason.substring(reason.indexOf(':') + 1).trim();
        }
        return switch (reason) {
            case "Description is too short" -> "商品描述过短，关键信息不足";
            case "No product image uploaded" -> "未上传商品图片，无法核验实物信息";
            case "Price is missing or invalid" -> "商品价格缺失或不合法";
            case "Title or description claims authenticity but no proof is described" -> "标题或描述宣称正品/原装，但未说明发票、证书等证明材料";
            case "Seller credit score is low" -> "卖家信用评分偏低";
            case "Seller has upheld reports in recent records" -> "卖家近两年存在举报成立记录";
            case "Price is extremely lower than original price" -> "售价极低，明显低于原价，存在异常定价风险";
            case "Stock is zero or missing" -> "库存为 0 或库存信息缺失";
            case "Secondhand condition level is missing" -> "二手商品未填写成色等级";
            case "Original price is missing, price discount cannot be verified" -> "二手商品未填写原价，无法核验折价合理性";
            case "Sale price is far lower than original price" -> "二手售价明显低于原价，存在异常低价风险";
            default -> reason;
        };
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

    private List<String> buildModelImageInputs(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }
        List<String> inputs = new ArrayList<>();
        for (String imageUrl : imageUrls) {
            if (inputs.size() >= MAX_LLM_IMAGE_COUNT) {
                break;
            }
            String input = toModelImageInput(imageUrl);
            if (StringUtils.hasText(input)) {
                inputs.add(input);
            }
        }
        return inputs;
    }

    private String toModelImageInput(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }
        String trimmed = imageUrl.trim();
        String normalized = trimmed.toLowerCase();
        if (normalized.startsWith("http://") || normalized.startsWith("https://") || normalized.startsWith("data:image/")) {
            return trimmed;
        }
        if (!trimmed.startsWith("/uploads/")) {
            return null;
        }
        try {
            Path uploadDir = Paths.get(uploadProperties.getDir()).toAbsolutePath().normalize();
            Path imagePath = uploadDir.resolve(trimmed.substring("/uploads/".length())).normalize();
            if (!imagePath.startsWith(uploadDir) || !Files.isRegularFile(imagePath)) {
                return null;
            }
            if (Files.size(imagePath) > MAX_LLM_IMAGE_BYTES) {
                return null;
            }
            String mimeType = Files.probeContentType(imagePath);
            if (!StringUtils.hasText(mimeType) || !mimeType.toLowerCase().startsWith("image/")) {
                mimeType = guessImageMimeType(imagePath.getFileName().toString());
            }
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(imagePath));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String guessImageMimeType(String filename) {
        String normalized = filename == null ? "" : filename.toLowerCase();
        if (normalized.endsWith(".jpg") || normalized.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (normalized.endsWith(".webp")) {
            return "image/webp";
        }
        if (normalized.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/png";
    }

    private void notifySellerIfProductAuditBlocked(ProductRiskAudit audit, String decision, String adminRemark) {
        if (audit == null || Objects.equals(decision, "APPROVED")) {
            return;
        }
        Long sellerUserId = resolveAuditSellerUserId(audit);
        if (sellerUserId == null) {
            return;
        }
        String action = Objects.equals(decision, "CHANGE_REQUESTED") ? "需要修改" : "审核未通过";
        String title = "商品" + action + "：" + safeProductName(audit.getProductName());
        StringBuilder content = new StringBuilder();
        content.append("您发布的商品“").append(safeProductName(audit.getProductName())).append("”").append(action)
                .append("，当前已暂时下架，不能出售。");
        if (StringUtils.hasText(adminRemark)) {
            content.append("管理员说明：").append(adminRemark.trim()).append("。");
        } else if (Objects.equals(decision, "CHANGE_REQUESTED")) {
            content.append("请在卖家工作台进入商品编辑页，修改商品描述、图片或其他信息后重新提交。");
        } else {
            content.append("如需继续出售，请在卖家工作台调整商品描述、图片或其他信息后联系管理员复核。");
        }
        notificationService.createNotification(sellerUserId, title, content.toString());
    }

    private Long resolveAuditSellerUserId(ProductRiskAudit audit) {
        if (audit.getSellerUserId() != null) {
            return audit.getSellerUserId();
        }
        if (Objects.equals(audit.getProductType(), "SECONDHAND")) {
            SecondhandProduct product = secondhandProductMapper.selectById(audit.getProductId());
            return product == null ? null : product.getSellerUserId();
        }
        if (Objects.equals(audit.getProductType(), "NEW")) {
            Product product = productMapper.selectById(audit.getProductId());
            if (product == null || product.getShopId() == null) {
                return null;
            }
            Shop shop = shopMapper.selectById(product.getShopId());
            return shop == null ? null : shop.getOwnerUserId();
        }
        return null;
    }

    private String safeProductName(String productName) {
        return StringUtils.hasText(productName) ? productName.trim() : "未命名商品";
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

    private record RiskEvaluation(int riskScore, String riskLevel, String suggestion, List<String> riskReasons) {

        private RiskEvaluation {
            riskScore = Math.max(0, Math.min(100, riskScore));
            riskLevel = StringUtils.hasText(riskLevel) ? riskLevel : "LOW";
            suggestion = StringUtils.hasText(suggestion) ? suggestion : "AUTO_PASS";
            riskReasons = riskReasons == null ? List.of() : riskReasons;
        }
    }

    @PreDestroy
    public void shutdownLargeModelAuditExecutor() {
        largeModelAuditExecutor.shutdown();
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

    private int countUpheldReportsIn2Years(Long userId) {
        return userReportMapper.countUpheldReportsIn2Years(userId, LocalDateTime.now().minusYears(2));
    }
}
