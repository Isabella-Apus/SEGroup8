package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.AddressSaveRequest;
import com.segroup8.platform.dto.MerchantApplicationSubmitRequest;
import com.segroup8.platform.dto.UserProfileUpdateRequest;
import com.segroup8.platform.entity.Address;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.AddressMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.MerchantApplicationService;
import com.segroup8.platform.service.UserService;
import com.segroup8.platform.vo.AddressVO;
import com.segroup8.platform.vo.MerchantApplicationVO;
import com.segroup8.platform.vo.UserVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final MerchantApplicationService merchantApplicationService;

    public UserServiceImpl(UserMapper userMapper,
            AddressMapper addressMapper,
            MerchantApplicationService merchantApplicationService) {
        this.userMapper = userMapper;
        this.addressMapper = addressMapper;
        this.merchantApplicationService = merchantApplicationService;
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
        vo.setCategory(user.getCategory());
        vo.setRegion(user.getRegion());
        vo.setBusinessHours(user.getBusinessHours());
        vo.setReturnPolicy(user.getReturnPolicy());
        vo.setShippingPolicy(user.getShippingPolicy());
        vo.setAnnouncement(user.getAnnouncement());
        return vo;
    }

    @Override
    public void updateCurrentUserProfile(UserProfileUpdateRequest request) {
        User update = new User();
        update.setId(requireUserId());
        update.setNickname(request.getNickname());
        update.setAvatar(request.getAvatar());
        update.setPhone(request.getPhone());
        update.setEmail(request.getEmail());
        update.setShopName(request.getShopName());
        update.setShopDesc(request.getShopDesc());
        update.setBannerUrl(request.getBannerUrl());
        update.setCategory(request.getCategory());
        update.setRegion(request.getRegion());
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

    private Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        return userId;
    }
}
