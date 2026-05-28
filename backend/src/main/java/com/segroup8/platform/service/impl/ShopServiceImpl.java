package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.RoleEnum;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.ShopDecorationSaveRequest;
import com.segroup8.platform.entity.Shop;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.CategoryService;
import com.segroup8.platform.service.SellerRatingAssembler;
import com.segroup8.platform.service.ShopService;
import com.segroup8.platform.vo.ShopPublicVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
public class ShopServiceImpl implements ShopService {

    private final ShopMapper shopMapper;
    private final UserMapper userMapper;
    private final CategoryService categoryService;
    private final SellerRatingAssembler sellerRatingAssembler;

    public ShopServiceImpl(ShopMapper shopMapper, UserMapper userMapper, CategoryService categoryService,
            SellerRatingAssembler sellerRatingAssembler) {
        this.shopMapper = shopMapper;
        this.userMapper = userMapper;
        this.categoryService = categoryService;
        this.sellerRatingAssembler = sellerRatingAssembler;
    }

    @Override
    public ShopPublicVO getPublicShop(Long shopId) {
        Shop shop = findActiveShop(shopId);
        if (shop == null) {
            throw new BusinessException(404, "店铺不存在或已关闭");
        }
        return toPublicVO(shop);
    }

    @Override
    public ShopPublicVO getCurrentSellerShop() {
        return toPublicVO(getCurrentSellerShopEntity());
    }

    @Override
    public ShopPublicVO saveCurrentSellerDecoration(ShopDecorationSaveRequest request) {
        Shop shop = getCurrentSellerShopEntity();
        shop.setDecorationJson(request.getDecorationJson());
        shopMapper.updateById(shop);
        return toPublicVO(shopMapper.selectById(shop.getId()));
    }

    private Shop getCurrentSellerShopEntity() {
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
            throw new BusinessException(403, "仅卖家可操作店铺");
        }
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getOwnerUserId, userId)
                .eq(Shop::getStatus, 1)
                .orderByDesc(Shop::getId)
                .last("limit 1"));
        if (shop == null) {
            throw new BusinessException(400, "未找到有效店铺");
        }
        return shop;
    }

    private Shop findActiveShop(Long shopId) {
        if (shopId == null) {
            return null;
        }
        return shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getId, shopId)
                .eq(Shop::getStatus, 1)
                .last("limit 1"));
    }

    private ShopPublicVO toPublicVO(Shop shop) {
        User owner = shop.getOwnerUserId() == null ? null : userMapper.selectById(shop.getOwnerUserId());
        ShopPublicVO vo = new ShopPublicVO();
        vo.setId(shop.getId());
        vo.setOwnerUserId(shop.getOwnerUserId());
        vo.setName(firstText(shop.getName(), owner == null ? null : owner.getShopName(), owner == null ? null : owner.getNickname()));
        vo.setDescription(firstText(owner == null ? null : owner.getShopDesc(), shop.getDescription()));
        vo.setLogo(firstText(owner == null ? null : owner.getAvatar(), shop.getLogo()));
        vo.setBannerUrl(owner == null ? null : owner.getBannerUrl());
        vo.setRegion(firstText(shop.getRegion(), owner == null ? null : owner.getRegion()));
        vo.setCategory(categoryText(owner == null ? null : owner.getCategory()));
        vo.setBusinessHours(owner == null ? null : owner.getBusinessHours());
        vo.setReturnPolicy(owner == null ? null : owner.getReturnPolicy());
        vo.setShippingPolicy(owner == null ? null : owner.getShippingPolicy());
        vo.setAnnouncement(owner == null ? null : owner.getAnnouncement());
        vo.setDecorationJson(shop.getDecorationJson());
        vo.setRating(sellerRatingAssembler.buildForShop(shop.getOwnerUserId()));
        return vo;
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String categoryText(String category) {
        if (!StringUtils.hasText(category)) {
            return null;
        }
        try {
            Integer categoryId = Integer.parseInt(category.trim());
            String name = categoryService.getCategoryName(categoryId);
            return StringUtils.hasText(name) ? name : category;
        } catch (NumberFormatException e) {
            return category;
        }
    }
}
