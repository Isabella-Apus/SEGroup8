package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.SecondhandOrderCreateRequest;
import com.segroup8.platform.dto.SecondhandProductPageQueryRequest;
import com.segroup8.platform.dto.SecondhandProductSaveRequest;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.OrderItem;
import com.segroup8.platform.entity.SecondhandProduct;
import com.segroup8.platform.entity.ProductAuction;
import com.segroup8.platform.entity.Address;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.AddressMapper;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.OrderItemMapper;
import com.segroup8.platform.mapper.ProductAuctionMapper;
import com.segroup8.platform.mapper.SecondhandProductMapper;
import com.segroup8.platform.mapper.UserBlockMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.BrowseHistoryService;
import com.segroup8.platform.service.CategoryService;
import com.segroup8.platform.service.ProductRiskAuditService;
import com.segroup8.platform.service.SellerRatingAssembler;
import com.segroup8.platform.service.SecondhandProductService;
import com.segroup8.platform.service.SecondhandTradeService;
import com.segroup8.platform.vo.OrderItemVO;
import com.segroup8.platform.vo.OrderVO;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.SecondhandSellerPublicVO;
import com.segroup8.platform.vo.SecondhandProductVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;

@Service
public class SecondhandProductServiceImpl implements SecondhandProductService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
    private static final int ON_SHELF = 1;
    private static final int OFF_SHELF = 2;
    private static final int ORDER_PENDING_PAY = 0;
    private static final String AUCTION_ONGOING = "ONGOING";
    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SecondhandProductMapper secondhandProductMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductAuctionMapper productAuctionMapper;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final BrowseHistoryService browseHistoryService;
    private final UserBlockMapper userBlockMapper;
    private final CategoryService categoryService;
    private final SecondhandTradeService secondhandTradeService;
    private final ProductRiskAuditService productRiskAuditService;
    private final SellerRatingAssembler sellerRatingAssembler;

    public SecondhandProductServiceImpl(SecondhandProductMapper secondhandProductMapper,
            OrderInfoMapper orderInfoMapper,
            OrderItemMapper orderItemMapper,
            ProductAuctionMapper productAuctionMapper,
            UserMapper userMapper,
            AddressMapper addressMapper,
            BrowseHistoryService browseHistoryService,
            UserBlockMapper userBlockMapper,
            CategoryService categoryService,
            SecondhandTradeService secondhandTradeService,
            ProductRiskAuditService productRiskAuditService,
            SellerRatingAssembler sellerRatingAssembler) {
        this.secondhandProductMapper = secondhandProductMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.productAuctionMapper = productAuctionMapper;
        this.userMapper = userMapper;
        this.addressMapper = addressMapper;
        this.browseHistoryService = browseHistoryService;
        this.userBlockMapper = userBlockMapper;
        this.categoryService = categoryService;
        this.secondhandTradeService = secondhandTradeService;
        this.productRiskAuditService = productRiskAuditService;
        this.sellerRatingAssembler = sellerRatingAssembler;
    }

    @Override
    public PageVO<SecondhandProductVO> pagePublicProducts(SecondhandProductPageQueryRequest request) {
        validatePriceRange(request.getMinPrice(), request.getMaxPrice());
        LambdaQueryWrapper<SecondhandProduct> wrapper = new LambdaQueryWrapper<>();
        List<Long> auctionProductIds = listOngoingAuctionProductIds();
        if (auctionProductIds.isEmpty()) {
            wrapper.eq(SecondhandProduct::getStatus, ON_SHELF);
        } else {
            wrapper.and(w -> w.eq(SecondhandProduct::getStatus, ON_SHELF)
                    .or()
                    .in(SecondhandProduct::getId, auctionProductIds));
        }
        appendCommonFilters(wrapper, request);
        applySort(wrapper, request.getSortBy());

        // 拉黑屏蔽：双向过滤（我拉黑的人 + 拉黑我的人 的商品都不显示）
        Long currentUserId = UserContext.getUserId();
        if (currentUserId != null) {
            List<Long> blockedByMe = userBlockMapper.listBlockedIds(currentUserId);
            List<Long> blockedMe   = userBlockMapper.listBlockerIds(currentUserId);
            java.util.Set<Long> hiddenSellerIds = new java.util.HashSet<>();
            hiddenSellerIds.addAll(blockedByMe);
            hiddenSellerIds.addAll(blockedMe);
            if (!hiddenSellerIds.isEmpty()) {
                wrapper.notIn(SecondhandProduct::getSellerUserId, hiddenSellerIds);
            }
        }

        Page<SecondhandProduct> page = secondhandProductMapper.selectPage(
                Page.of(request.getPageNum(), request.getPageSize()),
                wrapper);
        // 某些环境下（分页插件未生效/被禁用）selectPage 不会回填 total，导致前端无限滚动判断失效。
        if (page.getTotal() == 0 && !page.getRecords().isEmpty()) {
            page.setTotal(secondhandProductMapper.selectCount(wrapper));
        }
        return toPageVO(page);
    }

    @Override
    public PageVO<SecondhandProductVO> pagePublicSellerProducts(Long sellerUserId,
            SecondhandProductPageQueryRequest request) {
        validatePriceRange(request.getMinPrice(), request.getMaxPrice());
        if (userMapper.selectById(sellerUserId) == null) {
            throw new BusinessException(404, "卖家不存在");
        }
        LambdaQueryWrapper<SecondhandProduct> wrapper = new LambdaQueryWrapper<SecondhandProduct>()
                .eq(SecondhandProduct::getSellerUserId, sellerUserId);
        List<Long> auctionProductIds = listOngoingAuctionProductIds();
        if (auctionProductIds.isEmpty()) {
            wrapper.eq(SecondhandProduct::getStatus, ON_SHELF);
        } else {
            wrapper.and(w -> w.eq(SecondhandProduct::getStatus, ON_SHELF)
                    .or()
                    .in(SecondhandProduct::getId, auctionProductIds));
        }
        appendCommonFilters(wrapper, request);
        applySort(wrapper, request.getSortBy());
        Page<SecondhandProduct> page = secondhandProductMapper.selectPage(
                Page.of(request.getPageNum(), request.getPageSize()),
                wrapper);
        if (page.getTotal() == 0 && !page.getRecords().isEmpty()) {
            page.setTotal(secondhandProductMapper.selectCount(wrapper));
        }
        return toPageVO(page);
    }

    @Override
    public SecondhandSellerPublicVO getPublicSeller(Long sellerUserId) {
        User user = sellerUserId == null ? null : userMapper.selectById(sellerUserId);
        if (user == null) {
            throw new BusinessException(404, "卖家不存在");
        }
        SecondhandSellerPublicVO vo = new SecondhandSellerPublicVO();
        vo.setUserId(user.getId());
        vo.setNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        vo.setAvatar(user.getAvatar());
        vo.setRegion(user.getRegion());
        vo.setRating(sellerRatingAssembler.buildForSecondhand(user.getId()));
        return vo;
    }

    @Override
    public SecondhandProductVO getPublicProductDetail(Long productId) {
        SecondhandProduct product = secondhandProductMapper.selectOne(new LambdaQueryWrapper<SecondhandProduct>()
                .eq(SecondhandProduct::getId, productId)
                .last("limit 1"));
        if (product == null) {
            throw new BusinessException(404, "二手商品不存在或已下架");
        }
        browseHistoryService.saveBrowseHistory(productId, "SECONDHAND");
        return toVO(product);
    }

    @Override
    public PageVO<SecondhandProductVO> pageSellerProducts(SecondhandProductPageQueryRequest request) {
        validatePriceRange(request.getMinPrice(), request.getMaxPrice());
        Long sellerUserId = requireUserId();
        LambdaQueryWrapper<SecondhandProduct> wrapper = new LambdaQueryWrapper<SecondhandProduct>()
            .eq(SecondhandProduct::getSellerUserId, sellerUserId);
        appendCommonFilters(wrapper, request);
        if (request.getStatus() != null) {
            wrapper.eq(SecondhandProduct::getStatus, request.getStatus());
        }
        applySort(wrapper, request.getSortBy());
        Page<SecondhandProduct> page = secondhandProductMapper.selectPage(
                Page.of(request.getPageNum(), request.getPageSize()),
                wrapper);
        if (page.getTotal() == 0 && !page.getRecords().isEmpty()) {
            page.setTotal(secondhandProductMapper.selectCount(wrapper));
        }
        return toPageVO(page);
    }

    @Override
    public SecondhandProductVO createSellerProduct(SecondhandProductSaveRequest request) {
        validatePriceFields(request.getOriginPrice(), request.getSalePrice());
        validateSecondhandCategory(request.getCategoryId(), request.getSubCategoryId());
        Long sellerUserId = requireUserId();
        SecondhandProduct product = new SecondhandProduct();
        product.setSellerUserId(sellerUserId);
        product.setName(request.getName().trim());
        List<String> images = normalizeImages(request.getImages(), request.getCover());
        product.setCover(firstImage(images));
        product.setImages(serializeImages(images));
        product.setDescription(request.getDescription());
        product.setOriginPrice(request.getOriginPrice());
        product.setSalePrice(request.getSalePrice());
        product.setCategoryId(request.getCategoryId());
        product.setSubCategoryId(request.getSubCategoryId());
        product.setConditionLevel(request.getConditionLevel());
        product.setIsNegotiable(request.getIsNegotiable());
        product.setStatus(normalizeStatus(request.getStatus(), ON_SHELF));
        secondhandProductMapper.insert(product);
        productRiskAuditService.auditSecondhandProduct(secondhandProductMapper.selectById(product.getId()));
        return toVO(secondhandProductMapper.selectById(product.getId()));
    }

    @Override
    public SecondhandProductVO updateSellerProduct(Long productId, SecondhandProductSaveRequest request) {
        validatePriceFields(request.getOriginPrice(), request.getSalePrice());
        validateSecondhandCategory(request.getCategoryId(), request.getSubCategoryId());
        SecondhandProduct product = getSellerOwnedProduct(productId);
        product.setName(request.getName().trim());
        List<String> images = normalizeImages(request.getImages(), request.getCover());
        product.setCover(firstImage(images));
        product.setImages(serializeImages(images));
        product.setDescription(request.getDescription());
        product.setOriginPrice(request.getOriginPrice());
        product.setSalePrice(request.getSalePrice());
        product.setCategoryId(request.getCategoryId());
        product.setSubCategoryId(request.getSubCategoryId());
        product.setConditionLevel(request.getConditionLevel());
        product.setIsNegotiable(request.getIsNegotiable());
        if (request.getStatus() != null) {
            product.setStatus(normalizeStatus(request.getStatus(), null));
        }
        secondhandProductMapper.updateById(product);
        productRiskAuditService.auditSecondhandProduct(secondhandProductMapper.selectById(productId));
        return toVO(secondhandProductMapper.selectById(productId));
    }

    @Override
    public void deleteSellerProduct(Long productId) {
        SecondhandProduct product = getSellerOwnedProduct(productId);
        secondhandProductMapper.deleteById(product.getId());
    }

    @Override
    public SecondhandProductVO changeSellerProductStatus(Long productId, Integer status) {
        SecondhandProduct product = getSellerOwnedProduct(productId);
        product.setStatus(normalizeStatus(status, null));
        secondhandProductMapper.updateById(product);
        return toVO(secondhandProductMapper.selectById(productId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO buySecondhandProduct(Long productId, SecondhandOrderCreateRequest request) {
        Long buyerUserId = requireUserId();
        SecondhandProduct product = secondhandProductMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(404, "二手商品不存在");
        }
        if (Objects.equals(product.getSellerUserId(), buyerUserId)) {
            throw new BusinessException(400, "不能购买自己发布的二手商品");
        }
        if (!Objects.equals(product.getStatus(), ON_SHELF)) {
            throw new BusinessException(400, "二手商品已下架或已售出");
        }
        ProductAuction ongoingAuction = productAuctionMapper.selectOne(new LambdaQueryWrapper<ProductAuction>()
                .eq(ProductAuction::getProductId, productId)
                .eq(ProductAuction::getStatus, AUCTION_ONGOING)
                .last("limit 1"));
        if (ongoingAuction != null) {
            throw new BusinessException(400, "该商品正在拍卖中，暂不支持一口价购买");
        }        // 拉黑拦截：任意一方拉黑对方都不能交易
        Long sellerUserId = product.getSellerUserId();
        if (userBlockMapper.isBlocked(buyerUserId, sellerUserId) > 0) {
            throw new BusinessException(403, "您已拉黑该卖家，无法购买其商品");
        }
        if (userBlockMapper.isBlocked(sellerUserId, buyerUserId) > 0) {
            throw new BusinessException(403, "该卖家已拉黑您，无法购买其商品");
        }

        int updated = secondhandProductMapper.update(null, new UpdateWrapper<SecondhandProduct>()
                .set("status", OFF_SHELF)
                .eq("id", productId)
                .eq("status", ON_SHELF));
        if (updated == 0) {
            throw new BusinessException(400, "二手商品已售出");
        }

        BigDecimal effectivePrice = secondhandTradeService.resolveEffectivePriceForBuyer(productId, buyerUserId);
        BigDecimal dealPrice = effectivePrice == null ? product.getSalePrice() : effectivePrice;

        OrderInfo order = new OrderInfo();
        order.setOrderNo(generateOrderNo(buyerUserId));
        order.setBuyerUserId(buyerUserId);
        order.setTotalAmount(dealPrice);
        order.setVoucherDiscountAmount(BigDecimal.ZERO);
        order.setSellerBearAmount(BigDecimal.ZERO);
        order.setPlatformBearAmount(BigDecimal.ZERO);
        order.setPayableAmount(dealPrice);
        // 二手下单与普通商品一致，先进入待付款，再由买家主动支付推进状态。
        order.setPayStatus(0);
        order.setOrderStatus(ORDER_PENDING_PAY);
        order.setCanRefund(1);
        order.setLogisticsStatus("PENDING");
        order.setLogisticsCurrentIndex(0);
        order.setRemark(request == null ? null : request.getRemark());
        order.setCreateTime(LocalDateTime.now());

        Address addr;
        if (request != null && request.getAddressId() != null) {
            addr = addressMapper.selectById(request.getAddressId());
            if (addr == null || !Objects.equals(addr.getUserId(), buyerUserId)) {
                throw new BusinessException(400, "收货地址不存在或不属于当前用户");
            }
        } else {
            // 二手下单默认带入买家常用收货地址，保证订单详情信息完整。
            addr = addressMapper.selectOne(new LambdaQueryWrapper<Address>()
                    .eq(Address::getUserId, buyerUserId)
                    .orderByDesc(Address::getIsDefault)
                    .orderByDesc(Address::getId)
                    .last("limit 1"));
        }
        if (addr != null) {
            order.setReceiverName(addr.getReceiverName());
            order.setReceiverPhone(addr.getReceiverPhone());
            order.setReceiverProvince(addr.getProvince());
            order.setReceiverCity(addr.getCity());
            order.setReceiverDetailAddress(addr.getDetailAddress());
        }
        orderInfoMapper.insert(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setProductType("SECONDHAND");
        orderItem.setProductId(product.getId());
        orderItem.setProductName(product.getName());
        orderItem.setPrice(dealPrice);
        orderItem.setQuantity(1);
        orderItem.setStatus(1);
        orderItemMapper.insert(orderItem);

        secondhandTradeService.markNegotiationUsed(productId, buyerUserId, order.getId());

        return toOrderVO(order, orderItem);
    }

    private Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        return userId;
    }

    private void appendCommonFilters(LambdaQueryWrapper<SecondhandProduct> wrapper,
            SecondhandProductPageQueryRequest request) {
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(SecondhandProduct::getName, request.getKeyword().trim());
        }
        if (request.getMinPrice() != null) {
            wrapper.ge(SecondhandProduct::getSalePrice, request.getMinPrice());
        }
        if (request.getMaxPrice() != null) {
            wrapper.le(SecondhandProduct::getSalePrice, request.getMaxPrice());
        }
        if (request.getCategoryId() != null) {
            Set<Integer> leafIds = categoryService.resolveLeafCategoryIds(request.getCategoryId());
            if (leafIds.isEmpty()) {
                wrapper.eq(SecondhandProduct::getSubCategoryId, -1);
            } else {
                wrapper.in(SecondhandProduct::getSubCategoryId, leafIds);
            }
        }
        if (StringUtils.hasText(request.getConditionLevel())) {
            wrapper.eq(SecondhandProduct::getConditionLevel, request.getConditionLevel().trim());
        }
        if (request.getIsNegotiable() != null) {
            wrapper.eq(SecondhandProduct::getIsNegotiable, request.getIsNegotiable());
        }
    }

    private List<Long> listOngoingAuctionProductIds() {
        return productAuctionMapper.selectList(new LambdaQueryWrapper<ProductAuction>()
                        .eq(ProductAuction::getStatus, AUCTION_ONGOING)
                        .gt(ProductAuction::getEndTime, LocalDateTime.now()))
                .stream()
                .map(ProductAuction::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private boolean hasOngoingAuction(Long productId) {
        if (productId == null) {
            return false;
        }
        Long count = productAuctionMapper.selectCount(new LambdaQueryWrapper<ProductAuction>()
                .eq(ProductAuction::getProductId, productId)
                .eq(ProductAuction::getStatus, AUCTION_ONGOING)
                .gt(ProductAuction::getEndTime, LocalDateTime.now()));
        return count != null && count > 0;
    }

    private void applySort(LambdaQueryWrapper<SecondhandProduct> wrapper, String sortBy) {
        String rule = StringUtils.hasText(sortBy) ? sortBy.trim() : "time_desc";
        switch (rule) {
            case "price_asc" -> wrapper.orderByAsc(SecondhandProduct::getSalePrice)
                    .orderByDesc(SecondhandProduct::getCreateTime);
            case "price_desc" -> wrapper.orderByDesc(SecondhandProduct::getSalePrice)
                    .orderByDesc(SecondhandProduct::getCreateTime);
            case "sales_desc" -> wrapper.last(
                    "ORDER BY (SELECT IFNULL(SUM(oi.quantity), 0) FROM order_item oi WHERE oi.product_type = 'SECONDHAND' AND oi.product_id = secondhand_product.id) DESC, create_time DESC");
            default -> wrapper.orderByDesc(SecondhandProduct::getCreateTime);
        }
    }

    private void validateSecondhandCategory(Integer categoryId, Integer subCategoryId) {
        if (!categoryService.isMainCategory(categoryId)) {
            throw new BusinessException(400, "一级分类非法");
        }
        if (Objects.equals(categoryId, CategoryService.FOOD_MAIN_CATEGORY_ID)) {
            throw new BusinessException(400, "二手商品不支持食品类目");
        }
        if (!categoryService.isSubCategoryOf(categoryId, subCategoryId)) {
            throw new BusinessException(400, "二级分类不属于所选一级分类");
        }
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException(400, "最低价不能大于最高价");
        }
    }

    private void validatePriceFields(BigDecimal originPrice, BigDecimal salePrice) {
        if (originPrice != null && salePrice != null && originPrice.compareTo(salePrice) < 0) {
            throw new BusinessException(400, "原价不能低于售价");
        }
    }

    private Integer normalizeStatus(Integer status, Integer fallback) {
        Integer target = status == null ? fallback : status;
        if (!Objects.equals(target, ON_SHELF) && !Objects.equals(target, OFF_SHELF)) {
            throw new BusinessException(400, "二手商品状态非法");
        }
        return target;
    }

    private SecondhandProduct getSellerOwnedProduct(Long productId) {
        Long sellerUserId = requireUserId();
        SecondhandProduct product = secondhandProductMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(404, "二手商品不存在");
        }
        if (!Objects.equals(product.getSellerUserId(), sellerUserId)) {
            throw new BusinessException(403, "无权操作该二手商品");
        }
        return product;
    }

    private PageVO<SecondhandProductVO> toPageVO(Page<SecondhandProduct> page) {
        PageVO<SecondhandProductVO> vo = new PageVO<>();
        vo.setTotal(page.getTotal());
        vo.setPageNum(page.getCurrent());
        vo.setPageSize(page.getSize());
        vo.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return vo;
    }

    private SecondhandProductVO toVO(SecondhandProduct product) {
        SecondhandProductVO vo = new SecondhandProductVO();
        vo.setId(product.getId());
        vo.setSellerUserId(product.getSellerUserId());
        User seller = product.getSellerUserId() == null ? null : userMapper.selectById(product.getSellerUserId());
        if (seller != null) {
            vo.setSellerName(StringUtils.hasText(seller.getNickname()) ? seller.getNickname() : seller.getUsername());
        }
        vo.setName(product.getName());
        List<String> images = parseImages(product.getImages(), product.getCover());
        vo.setCover(firstImage(images));
        vo.setImages(images);
        vo.setDescription(product.getDescription());
        vo.setOriginPrice(product.getOriginPrice());
        vo.setSalePrice(product.getSalePrice());
        vo.setCategoryId(product.getCategoryId());
        vo.setSubCategoryId(product.getSubCategoryId());
        vo.setCategoryName(categoryService.getCategoryName(product.getCategoryId()));
        vo.setSubCategoryName(categoryService.getCategoryName(product.getSubCategoryId()));
        vo.setConditionLevel(product.getConditionLevel());
        vo.setIsNegotiable(product.getIsNegotiable());
        vo.setStatus(product.getStatus());
        vo.setStatusName(Objects.equals(product.getStatus(), ON_SHELF) ? "在售" : "下架");
        vo.setRiskAudit(productRiskAuditService.getLatestAudit("SECONDHAND", product.getId()));
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

    private String generateOrderNo(Long userId) {
        String timePart = LocalDateTime.now().format(ORDER_NO_FORMATTER);
        return "SND" + timePart + String.format("%04d", userId % 10000);
    }

    private OrderVO toOrderVO(OrderInfo order, OrderItem item) {
        OrderItemVO itemVO = new OrderItemVO();
        itemVO.setId(item.getId());
        itemVO.setProductId(item.getProductId());
        itemVO.setProductName(item.getProductName());
        itemVO.setPrice(item.getPrice());
        itemVO.setQuantity(item.getQuantity());

        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayableAmount(order.getPayableAmount());
        vo.setPayStatus(order.getPayStatus());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(List.of(itemVO));
        return vo;
    }
}
