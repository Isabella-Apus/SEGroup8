package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.AddressSaveRequest;
import com.segroup8.platform.dto.MerchantApplicationSubmitRequest;
import com.segroup8.platform.dto.UserProfileUpdateRequest;
import com.segroup8.platform.entity.Address;
import com.segroup8.platform.entity.MerchantApplication;
import com.segroup8.platform.entity.Shop;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.AddressMapper;
import com.segroup8.platform.mapper.MerchantApplicationMapper;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.CategoryService;
import com.segroup8.platform.service.MerchantApplicationService;
import com.segroup8.platform.service.UserService;
import com.segroup8.platform.vo.AddressVO;
import com.segroup8.platform.vo.MerchantApplicationVO;
import com.segroup8.platform.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final MerchantApplicationService merchantApplicationService;
    private final ShopMapper shopMapper;
    private final MerchantApplicationMapper merchantApplicationMapper;
    private final CategoryService categoryService;

    @Autowired
    public UserServiceImpl(UserMapper userMapper,
            AddressMapper addressMapper,
            MerchantApplicationService merchantApplicationService,
            ShopMapper shopMapper,
            MerchantApplicationMapper merchantApplicationMapper,
            CategoryService categoryService) {
        this.userMapper = userMapper;
        this.addressMapper = addressMapper;
        this.merchantApplicationService = merchantApplicationService;
        this.shopMapper = shopMapper;
        this.merchantApplicationMapper = merchantApplicationMapper;
        this.categoryService = categoryService;
    }

    public UserServiceImpl(UserMapper userMapper,
            AddressMapper addressMapper,
            MerchantApplicationService merchantApplicationService,
            ShopMapper shopMapper,
            MerchantApplicationMapper merchantApplicationMapper) {
        this(userMapper, addressMapper, merchantApplicationService, shopMapper, merchantApplicationMapper, null);
    }

    @Override
    public UserVO getCurrentUserProfile() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreditScore(user.getCreditScore());
        vo.setShopName(user.getShopName());
        vo.setShopDesc(user.getShopDesc());
        vo.setBannerUrl(user.getBannerUrl());
        Integer userCategoryId = parseCategoryId(user.getCategory());
        vo.setCategoryId(userCategoryId);
        vo.setCategory(userCategoryId == null ? user.getCategory() : categoryName(userCategoryId));
        vo.setRegion(user.getRegion());
        vo.setBusinessHours(user.getBusinessHours());
        vo.setReturnPolicy(user.getReturnPolicy());
        vo.setShippingPolicy(user.getShippingPolicy());
        vo.setAnnouncement(user.getAnnouncement());

        Shop shop = findLatestShopByOwner(userId);
        MerchantApplication approvedApp = findLatestApprovedApplication(userId);
        if (shop == null && approvedApp != null) {
            shop = new Shop();
            shop.setOwnerUserId(userId);
            shop.setStatus(1);
            applyShopBackfill(shop, approvedApp);
            shopMapper.insert(shop);
        } else if (shop != null && approvedApp != null) {
            boolean changed = applyShopBackfillIfBlank(shop, approvedApp);
            if (changed) {
                shopMapper.updateById(shop);
            }
        }

        if (shop != null) {
            if (StringUtils.hasText(shop.getName())) {
                vo.setShopName(shop.getName());
            }
            if (StringUtils.hasText(shop.getRegion())) {
                vo.setRegion(shop.getRegion());
            }
            vo.setShopContactName(shop.getContactName());
            vo.setShopContactPhone(shop.getContactPhone());
            vo.setWarehouseAddr(shop.getWarehouseAddr());
            vo.setIdCardNoMasked(shop.getIdCardNoMasked());
        }

        if ((vo.getCategoryId() == null || !StringUtils.hasText(vo.getCategory())) && approvedApp != null) {
            vo.setCategoryId(approvedApp.getCategoryId());
            vo.setCategory(categoryName(approvedApp.getCategoryId()));
        }
        return vo;
    }

    @Override
    public List<UserVO> searchUsers(String keyword) {
        Long currentUserId = requireUserId();
        String normalized = StringUtils.hasText(keyword) ? keyword.trim() : "";
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .ne(User::getId, currentUserId)
                .ne(User::getRole, "ADMIN")
                .orderByDesc(User::getId)
                .last("limit 10");
        if (StringUtils.hasText(normalized)) {
            Long id = parseLong(normalized);
            wrapper.and(query -> {
                if (id != null) {
                    query.eq(User::getId, id)
                            .or();
                }
                query.like(User::getUsername, normalized)
                        .or()
                        .like(User::getNickname, normalized);
            });
        }
        return userMapper.selectList(wrapper)
                .stream()
                .map(this::toPublicUserVO)
                .toList();
    }

    @Override
    public void updateCurrentUserProfile(UserProfileUpdateRequest request) {
        User update = new User();
        update.setId(requireUserId());
        update.setNickname(request.getNickname());
        update.setAvatar(request.getAvatar());
        update.setPhone(request.getPhone());
        update.setEmail(request.getEmail());
        update.setShopDesc(request.getShopDesc());
        update.setBannerUrl(request.getBannerUrl());
        update.setBusinessHours(request.getBusinessHours());
        update.setReturnPolicy(request.getReturnPolicy());
        update.setShippingPolicy(request.getShippingPolicy());
        update.setAnnouncement(request.getAnnouncement());
        userMapper.updateById(update);
    }

    @Override
    public List<AddressVO> listMyAddresses() {
        Long userId = requireUserId();
        return addressMapper.selectList(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getId))
                .stream()
                .map(this::toAddressVO)
                .toList();
    }

    @Override
    public void createAddress(AddressSaveRequest request) {
        Long userId = requireUserId();
        if (request.getIsDefault() != null && request.getIsDefault() == 1) {
            clearDefaultAddress(userId);
        }
        Address address = new Address();
        address.setUserId(userId);
        applyAddressFields(address, request);
        addressMapper.insert(address);
    }

    @Override
    public void updateAddress(Long addressId, AddressSaveRequest request) {
        Long userId = requireUserId();
        Address address = getOwnedAddress(userId, addressId);
        if (request.getIsDefault() != null && request.getIsDefault() == 1) {
            clearDefaultAddress(userId);
        }
        applyAddressFields(address, request);
        addressMapper.updateById(address);
    }

    @Override
    public void deleteAddress(Long addressId) {
        Long userId = requireUserId();
        Address address = getOwnedAddress(userId, addressId);
        addressMapper.deleteById(address.getId());
    }

    @Override
    public void submitMerchantApplication(MerchantApplicationSubmitRequest request) {
        merchantApplicationService.submit(request);
    }

    @Override
    public MerchantApplicationVO getMyMerchantApplication() {
        return merchantApplicationService.getMyApplication();
    }

    private Address getOwnedAddress(Long userId, Long addressId) {
        Address address = addressMapper.selectById(addressId);
        if (address == null || !userId.equals(address.getUserId())) {
            throw new BusinessException(404, "地址不存在");
        }
        return address;
    }

    private void clearDefaultAddress(Long userId) {
        Address update = new Address();
        update.setIsDefault(0);
        addressMapper.update(update, new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, 1));
    }

    private void applyAddressFields(Address address, AddressSaveRequest request) {
        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setDetailAddress(request.getDetailAddress());
        address.setIsDefault(request.getIsDefault());
    }

    private AddressVO toAddressVO(Address address) {
        AddressVO vo = new AddressVO();
        vo.setId(address.getId());
        vo.setReceiverName(address.getReceiverName());
        vo.setReceiverPhone(address.getReceiverPhone());
        vo.setProvince(address.getProvince());
        vo.setCity(address.getCity());
        vo.setDetailAddress(address.getDetailAddress());
        vo.setIsDefault(address.getIsDefault());
        return vo;
    }

    private UserVO toPublicUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setShopName(user.getShopName());
        return vo;
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        return userId;
    }

    private Shop findLatestShopByOwner(Long userId) {
        return shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getOwnerUserId, userId)
                .orderByDesc(Shop::getId)
                .last("limit 1"));
    }

    private MerchantApplication findLatestApprovedApplication(Long userId) {
        return merchantApplicationMapper.selectOne(new LambdaQueryWrapper<MerchantApplication>()
                .eq(MerchantApplication::getUserId, userId)
                .eq(MerchantApplication::getStatus, 1)
                .orderByDesc(MerchantApplication::getId)
                .last("limit 1"));
    }

    private void applyShopBackfill(Shop shop, MerchantApplication app) {
        shop.setName(app.getStoreName());
        shop.setRegion(buildRegion(app.getWarehouseProvince(), app.getWarehouseCity()));
        shop.setContactName(app.getContactName());
        shop.setContactPhone(app.getContactPhone());
        shop.setIdCardNoMasked(maskMiddle(app.getIdCardNo()));
        shop.setWarehouseAddr(buildWarehouseAddr(
                app.getWarehouseProvince(),
                app.getWarehouseCity(),
                app.getWarehouseDetail(),
                app.getWarehouseAddr()));
    }

    private boolean applyShopBackfillIfBlank(Shop shop, MerchantApplication app) {
        boolean changed = false;
        if (!StringUtils.hasText(shop.getName()) && StringUtils.hasText(app.getStoreName())) {
            shop.setName(app.getStoreName());
            changed = true;
        }
        String region = buildRegion(app.getWarehouseProvince(), app.getWarehouseCity());
        if (!StringUtils.hasText(shop.getRegion()) && StringUtils.hasText(region)) {
            shop.setRegion(region);
            changed = true;
        }
        if (!StringUtils.hasText(shop.getContactName()) && StringUtils.hasText(app.getContactName())) {
            shop.setContactName(app.getContactName());
            changed = true;
        }
        if (!StringUtils.hasText(shop.getContactPhone()) && StringUtils.hasText(app.getContactPhone())) {
            shop.setContactPhone(app.getContactPhone());
            changed = true;
        }
        String maskedIdCard = maskMiddle(app.getIdCardNo());
        if (!StringUtils.hasText(shop.getIdCardNoMasked()) && StringUtils.hasText(maskedIdCard)) {
            shop.setIdCardNoMasked(maskedIdCard);
            changed = true;
        }
        String warehouseAddr = buildWarehouseAddr(
                app.getWarehouseProvince(),
                app.getWarehouseCity(),
                app.getWarehouseDetail(),
                app.getWarehouseAddr());
        if (!StringUtils.hasText(shop.getWarehouseAddr()) && StringUtils.hasText(warehouseAddr)) {
            shop.setWarehouseAddr(warehouseAddr);
            changed = true;
        }
        return changed;
    }

    private String buildRegion(String province, String city) {
        if (StringUtils.hasText(province) && StringUtils.hasText(city)) {
            return province + " " + city;
        }
        return StringUtils.hasText(province) ? province : city;
    }

    private String buildWarehouseAddr(String province, String city, String detail, String fallback) {
        if (StringUtils.hasText(province) && StringUtils.hasText(city) && StringUtils.hasText(detail)) {
            return province + " " + city + " " + detail;
        }
        return fallback;
    }

    private String maskMiddle(String val) {
        if (!StringUtils.hasText(val) || val.length() <= 6) {
            return val;
        }
        return val.substring(0, 3) + "****" + val.substring(val.length() - 3);
    }

    private Integer parseCategoryId(String category) {
        if (!StringUtils.hasText(category)) {
            return null;
        }
        try {
            return Integer.parseInt(category.trim());
        } catch (NumberFormatException e) {
            return null;
        }
/*
        return switch (categoryId) {
            case 1 -> "食品";
            case 2 -> "3C";
            case 3 -> "美妆";
            case 4 -> "服装";
            case 5 -> "运动";
            default -> String.valueOf(categoryId);
        };
*/
    }

    private String categoryName(Integer categoryId) {
        if (categoryId == null) {
            return null;
        }
        if (categoryService != null) {
            String name = categoryService.getCategoryName(categoryId);
            if (StringUtils.hasText(name)) {
                return name;
            }
        }
        return switch (categoryId) {
            case 1 -> "电子数码";
            case 2 -> "服饰鞋包";
            case 3 -> "家居生活";
            case 4 -> "美妆个护";
            case 5 -> "运动户外";
            case 6 -> "图书音像";
            case 7 -> "食品生鲜";
            case 8 -> "其他";
            default -> String.valueOf(categoryId);
        };
    }
}
