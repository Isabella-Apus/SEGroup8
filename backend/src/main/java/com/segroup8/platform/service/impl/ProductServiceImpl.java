package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.ProductStatusEnum;
import com.segroup8.platform.common.RoleEnum;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.ProductPageQueryRequest;
import com.segroup8.platform.dto.ProductSaveRequest;
import com.segroup8.platform.entity.Product;
import com.segroup8.platform.entity.Shop;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.ProductMapper;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.mapper.UserBlockMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.BrowseHistoryService;
import com.segroup8.platform.service.CategoryService;
import com.segroup8.platform.service.ProductService;
import com.segroup8.platform.service.ProductRiskAuditService;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.ProductVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private final ProductMapper productMapper;
    private final ShopMapper shopMapper;
    private final UserMapper userMapper;
    private final UserBlockMapper userBlockMapper;
    private final BrowseHistoryService browseHistoryService;
    private final CategoryService categoryService;
    private final ProductRiskAuditService productRiskAuditService;

    public ProductServiceImpl(ProductMapper productMapper, ShopMapper shopMapper, UserMapper userMapper,
            UserBlockMapper userBlockMapper,
            BrowseHistoryService browseHistoryService,
            CategoryService categoryService,
            ProductRiskAuditService productRiskAuditService) {
        this.productMapper = productMapper;
        this.shopMapper = shopMapper;
        this.userMapper = userMapper;
        this.userBlockMapper = userBlockMapper;
        this.browseHistoryService = browseHistoryService;
        this.categoryService = categoryService;
        this.productRiskAuditService = productRiskAuditService;
    }

    @Override
    public PageVO<ProductVO> pagePublicProducts(ProductPageQueryRequest request) {
        validatePriceRange(request.getMinPrice(), request.getMaxPrice());
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
            .eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode());
        appendCommonFilters(wrapper, request);
        appendMutualBlockFilter(wrapper);
        applySort(wrapper, request.getSortBy());
        Page<Product> page = productMapper.selectPage(Page.of(request.getPageNum(), request.getPageSize()), wrapper);
        return toPageVO(page);
    }

    @Override
    public PageVO<ProductVO> pagePublicShopProducts(Long shopId, ProductPageQueryRequest request) {
        validatePriceRange(request.getMinPrice(), request.getMaxPrice());
        Shop shop = shopId == null ? null : shopMapper.selectById(shopId);
        if (shop == null || !Objects.equals(shop.getStatus(), 1)) {
            throw new BusinessException(404, "店铺不存在或已关闭");
        }
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getShopId, shopId)
                .eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode());
        appendCommonFilters(wrapper, request);
        appendMutualBlockFilter(wrapper);
        applySort(wrapper, request.getSortBy());
        Page<Product> page = productMapper.selectPage(Page.of(request.getPageNum(), request.getPageSize()), wrapper);
        return toPageVO(page);
    }

    @Override
    public ProductVO getPublicProductDetail(Long productId) {
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode())
                .last("limit 1"));
        if (product == null) {
            throw new BusinessException(404, "商品不存在或已下架");
        }
        assertMutualBlockAllowed(product);
        browseHistoryService.saveBrowseHistory(productId, "NEW");
        return toVO(product);
    }

    @Override
    public ProductVO getSellerProductDetail(Long productId) {
        return toVO(getSellerOwnedProduct(productId));
    }

    @Override
    public PageVO<ProductVO> pageSellerProducts(ProductPageQueryRequest request) {
        validatePriceRange(request.getMinPrice(), request.getMaxPrice());
        Long shopId = getCurrentSellerShopId();
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getShopId, shopId);
        appendCommonFilters(wrapper, request);
        if (request.getStatus() != null) {
            wrapper.eq(Product::getStatus, request.getStatus());
        }
        applySort(wrapper, request.getSortBy());
        Page<Product> page = productMapper.selectPage(Page.of(request.getPageNum(), request.getPageSize()), wrapper);
        return toPageVO(page);
    }

    @Override
    public ProductVO createSellerProduct(ProductSaveRequest request) {
        Long shopId = getCurrentSellerShopId();
        validateCategoryForSeller(request.getCategoryId(), request.getSubCategoryId());
        Integer targetStatus = normalizeStatus(request.getStatus(), ProductStatusEnum.ON_SHELF.getCode());

        Product product = new Product();
        product.setShopId(shopId);
        product.setName(request.getName().trim());
        List<String> images = normalizeImages(request.getImages(), request.getCover());
        product.setCover(firstImage(images));
        product.setImages(serializeImages(images));
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategoryId(request.getCategoryId());
        product.setSubCategoryId(request.getSubCategoryId());
        product.setStock(request.getStock());
        product.setStatus(targetStatus);
        productMapper.insert(product);
        productRiskAuditService.auditNewProduct(productMapper.selectById(product.getId()));
        return toVO(productMapper.selectById(product.getId()));
    }

    @Override
    public ProductVO updateSellerProduct(Long productId, ProductSaveRequest request) {
        Product product = getSellerOwnedProduct(productId);
        validateCategoryForSeller(request.getCategoryId(), request.getSubCategoryId());
        Integer targetStatus = request.getStatus() == null ? product.getStatus()
                : normalizeStatus(request.getStatus(), null);

        product.setName(request.getName().trim());
        List<String> images = normalizeImages(request.getImages(), request.getCover());
        product.setCover(firstImage(images));
        product.setImages(serializeImages(images));
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategoryId(request.getCategoryId());
        product.setSubCategoryId(request.getSubCategoryId());
        product.setStock(request.getStock());
        product.setStatus(targetStatus);
        productMapper.updateById(product);
        productRiskAuditService.auditNewProduct(productMapper.selectById(product.getId()));
        return toVO(productMapper.selectById(product.getId()));
    }

    @Override
    public void deleteSellerProduct(Long productId) {
        Product product = getSellerOwnedProduct(productId);
        productMapper.deleteById(product.getId());
    }

    @Override
    public ProductVO changeSellerProductStatus(Long productId, Integer status) {
        Product product = getSellerOwnedProduct(productId);
        product.setStatus(normalizeStatus(status, null));
        productMapper.updateById(product);
        return toVO(productMapper.selectById(product.getId()));
    }

    @Override
    public ProductVO adjustSellerProductStock(Long productId, Integer delta) {
        if (delta == null || delta == 0) {
            throw new BusinessException(400, "库存变更值不能为0");
        }
        Product product = getSellerOwnedProduct(productId);
        int nextStock = product.getStock() + delta;
        if (nextStock < 0) {
            throw new BusinessException(400, "库存不足，无法扣减");
        }
        int updated = productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .set(Product::getStock, nextStock)
                .eq(Product::getId, productId));
        if (updated == 0) {
            throw new BusinessException(500, "库存更新失败");
        }
        return toVO(productMapper.selectById(productId));
    }

    private void appendCommonFilters(LambdaQueryWrapper<Product> wrapper, ProductPageQueryRequest request) {
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(Product::getName, request.getKeyword().trim());
        }
        if (request.getMinPrice() != null) {
            wrapper.ge(Product::getPrice, request.getMinPrice());
        }
        if (request.getMaxPrice() != null) {
            wrapper.le(Product::getPrice, request.getMaxPrice());
        }
        if (request.getCategoryId() != null) {
            Set<Integer> leafIds = categoryService.resolveLeafCategoryIds(request.getCategoryId());
            if (leafIds.isEmpty()) {
                wrapper.eq(Product::getSubCategoryId, -1);
            } else {
                wrapper.in(Product::getSubCategoryId, leafIds);
            }
        }
    }

    private void appendMutualBlockFilter(LambdaQueryWrapper<Product> wrapper) {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            return;
        }
        java.util.Set<Long> hiddenSellerIds = new java.util.HashSet<>();
        hiddenSellerIds.addAll(userBlockMapper.listBlockedIds(currentUserId));
        hiddenSellerIds.addAll(userBlockMapper.listBlockerIds(currentUserId));
        if (hiddenSellerIds.isEmpty()) {
            return;
        }
        List<Long> hiddenShopIds = shopMapper.selectList(new LambdaQueryWrapper<Shop>()
                        .in(Shop::getOwnerUserId, hiddenSellerIds))
                .stream()
                .map(Shop::getId)
                .filter(Objects::nonNull)
                .toList();
        if (!hiddenShopIds.isEmpty()) {
            wrapper.notIn(Product::getShopId, hiddenShopIds);
        }
    }

    private void assertMutualBlockAllowed(Product product) {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null || product == null || product.getShopId() == null) {
            return;
        }
        Shop shop = shopMapper.selectById(product.getShopId());
        Long sellerUserId = shop == null ? null : shop.getOwnerUserId();
        if (sellerUserId == null) {
            return;
        }
        if (userBlockMapper.isBlocked(currentUserId, sellerUserId) > 0) {
            throw new BusinessException(403, "您已拉黑该卖家，无法查看其商品");
        }
        if (userBlockMapper.isBlocked(sellerUserId, currentUserId) > 0) {
            throw new BusinessException(403, "该卖家已拉黑您，无法查看其商品");
        }
    }

    private void applySort(LambdaQueryWrapper<Product> wrapper, String sortBy) {
        String rule = StringUtils.hasText(sortBy) ? sortBy.trim() : "time_desc";
        switch (rule) {
            case "price_asc" -> wrapper.orderByAsc(Product::getPrice).orderByDesc(Product::getCreateTime);
            case "price_desc" -> wrapper.orderByDesc(Product::getPrice).orderByDesc(Product::getCreateTime);
            case "sales_desc" -> wrapper.last(
                    "ORDER BY (SELECT IFNULL(SUM(oi.quantity), 0) FROM order_item oi WHERE oi.product_type = 'NEW' AND oi.product_id = product.id) DESC, create_time DESC");
            default -> wrapper.orderByDesc(Product::getCreateTime);
        }
    }

    private void validateCategoryForSeller(Integer categoryId, Integer subCategoryId) {
        if (!categoryService.isMainCategory(categoryId)) {
            throw new BusinessException(400, "一级分类非法");
        }
        if (!categoryService.isSubCategoryOf(categoryId, subCategoryId)) {
            throw new BusinessException(400, "二级分类不属于所选一级分类");
        }
        Integer sellerMainCategoryId = getCurrentSellerMainCategoryId();
        if (sellerMainCategoryId == null || !sellerMainCategoryId.equals(categoryId)) {
            throw new BusinessException(400, "仅允许发布店铺主营一级类目下的商品");
        }
    }

    private void validatePriceRange(java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException(400, "最低价不能大于最高价");
        }
    }

    private Integer normalizeStatus(Integer status, Integer fallback) {
        Integer target = status == null ? fallback : status;
        ProductStatusEnum statusEnum = ProductStatusEnum.of(target);
        if (statusEnum == null) {
            throw new BusinessException(400, "商品状态非法");
        }
        return statusEnum.getCode();
    }

    private Product getSellerOwnedProduct(Long productId) {
        Long shopId = getCurrentSellerShopId();
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if (!Objects.equals(product.getShopId(), shopId)) {
            throw new BusinessException(403, "无权操作该商品");
        }
        return product;
    }

    private Long getCurrentSellerShopId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        String role = user.getRole();
        if (!Objects.equals(role, RoleEnum.OFFICIAL_SELLER.name()) && !Objects.equals(role, RoleEnum.SELLER.name())) {
            throw new BusinessException(403, "仅卖家可操作商品");
        }
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getOwnerUserId, userId)
                .eq(Shop::getStatus, 1)
                .orderByDesc(Shop::getId)
                .last("limit 1"));
        if (shop == null) {
            throw new BusinessException(400, "未找到有效店铺，请联系管理员");
        }
        return shop.getId();
    }

    private Integer getCurrentSellerMainCategoryId() {
        Long userId = UserContext.getUserId();
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null || !StringUtils.hasText(user.getCategory())) {
            return null;
        }
        try {
            return Integer.parseInt(user.getCategory().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private PageVO<ProductVO> toPageVO(Page<Product> page) {
        PageVO<ProductVO> vo = new PageVO<>();
        vo.setTotal(page.getTotal());
        vo.setPageNum(page.getCurrent());
        vo.setPageSize(page.getSize());
        vo.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return vo;
    }

    private ProductVO toVO(Product product) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setShopId(product.getShopId());
        Shop shop = product.getShopId() == null ? null : shopMapper.selectById(product.getShopId());
        if (shop != null) {
            vo.setSellerUserId(shop.getOwnerUserId());
            User seller = shop.getOwnerUserId() == null ? null : userMapper.selectById(shop.getOwnerUserId());
            if (seller != null) {
                vo.setSellerName(StringUtils.hasText(seller.getNickname()) ? seller.getNickname() : seller.getUsername());
            } else {
                vo.setSellerName(shop.getName());
            }
        }
        vo.setName(product.getName());
        List<String> images = parseImages(product.getImages(), product.getCover());
        vo.setCover(firstImage(images));
        vo.setImages(images);
        vo.setDescription(product.getDescription());
        vo.setPrice(product.getPrice());
        vo.setCategoryId(product.getCategoryId());
        vo.setSubCategoryId(product.getSubCategoryId());
        vo.setCategoryName(categoryService.getCategoryName(product.getCategoryId()));
        vo.setSubCategoryName(categoryService.getCategoryName(product.getSubCategoryId()));
        vo.setStock(product.getStock());
        vo.setStatus(product.getStatus());
        ProductStatusEnum statusEnum = ProductStatusEnum.of(product.getStatus());
        vo.setStatusName(statusEnum == null ? "未知" : statusEnum.getDesc());
        vo.setRiskAudit(productRiskAuditService.getLatestAudit("NEW", product.getId()));
        vo.setCreateTime(product.getCreateTime());
        return vo;
    }

    private List<String> normalizeImages(List<String> images, String cover) {
        List<String> normalized = new ArrayList<>();
        if (images != null) {
            for (String image : images) {
                if (StringUtils.hasText(image) && !normalized.contains(image.trim())) {
                    normalized.add(image.trim());
                }
            }
        }
        if (normalized.isEmpty() && StringUtils.hasText(cover)) {
            normalized.add(cover.trim());
        }
        if (normalized.size() > 9) {
            throw new BusinessException(400, "商品图片不能超过9张");
        }
        return normalized;
    }

    private List<String> parseImages(String imagesJson, String cover) {
        if (StringUtils.hasText(imagesJson)) {
            try {
                return normalizeImages(OBJECT_MAPPER.readValue(imagesJson, STRING_LIST_TYPE), cover);
            } catch (Exception ignored) {
                return normalizeImages(Collections.emptyList(), cover);
            }
        }
        return normalizeImages(Collections.emptyList(), cover);
    }

    private String serializeImages(List<String> images) {
        try {
            return OBJECT_MAPPER.writeValueAsString(images == null ? Collections.emptyList() : images);
        } catch (Exception e) {
            throw new BusinessException(500, "商品图片保存失败");
        }
    }

    private String firstImage(List<String> images) {
        return images == null || images.isEmpty() ? null : images.get(0);
    }
}
