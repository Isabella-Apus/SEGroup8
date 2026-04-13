package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.BrowseHistoryService;
import com.segroup8.platform.service.ProductService;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.ProductVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final ShopMapper shopMapper;
    private final UserMapper userMapper;
    private final BrowseHistoryService browseHistoryService;

    public ProductServiceImpl(ProductMapper productMapper, ShopMapper shopMapper, UserMapper userMapper,
            BrowseHistoryService browseHistoryService) {
        this.productMapper = productMapper;
        this.shopMapper = shopMapper;
        this.userMapper = userMapper;
        this.browseHistoryService = browseHistoryService;
    }

    @Override
    public PageVO<ProductVO> pagePublicProducts(ProductPageQueryRequest request) {
        validatePriceRange(request.getMinPrice(), request.getMaxPrice());
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode())
                .orderByDesc(Product::getCreateTime);
        appendCommonFilters(wrapper, request);
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
        browseHistoryService.saveBrowseHistory(productId, "NEW");
        return toVO(product);
    }

    @Override
    public PageVO<ProductVO> pageSellerProducts(ProductPageQueryRequest request) {
        validatePriceRange(request.getMinPrice(), request.getMaxPrice());
        Long shopId = getCurrentSellerShopId();
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getShopId, shopId)
                .orderByDesc(Product::getCreateTime);
        appendCommonFilters(wrapper, request);
        if (request.getStatus() != null) {
            wrapper.eq(Product::getStatus, request.getStatus());
        }
        Page<Product> page = productMapper.selectPage(Page.of(request.getPageNum(), request.getPageSize()), wrapper);
        return toPageVO(page);
    }

    @Override
    public ProductVO createSellerProduct(ProductSaveRequest request) {
        Long shopId = getCurrentSellerShopId();
        Integer targetStatus = normalizeStatus(request.getStatus(), ProductStatusEnum.ON_SHELF.getCode());

        Product product = new Product();
        product.setShopId(shopId);
        product.setName(request.getName().trim());
        product.setCover(request.getCover());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setStatus(targetStatus);
        productMapper.insert(product);
        return toVO(productMapper.selectById(product.getId()));
    }

    @Override
    public ProductVO updateSellerProduct(Long productId, ProductSaveRequest request) {
        Product product = getSellerOwnedProduct(productId);
        Integer targetStatus = request.getStatus() == null ? product.getStatus()
                : normalizeStatus(request.getStatus(), null);

        product.setName(request.getName().trim());
        product.setCover(request.getCover());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setStatus(targetStatus);
        productMapper.updateById(product);
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
        vo.setName(product.getName());
        vo.setCover(product.getCover());
        vo.setDescription(product.getDescription());
        vo.setPrice(product.getPrice());
        vo.setStock(product.getStock());
        vo.setStatus(product.getStatus());
        ProductStatusEnum statusEnum = ProductStatusEnum.of(product.getStatus());
        vo.setStatusName(statusEnum == null ? "未知" : statusEnum.getDesc());
        vo.setCreateTime(product.getCreateTime());
        return vo;
    }
}
