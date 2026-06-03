package com.segroup8.platform.service;

import com.segroup8.platform.dto.AddressSaveRequest;
import com.segroup8.platform.dto.MerchantApplicationSubmitRequest;
import com.segroup8.platform.dto.UserProfileUpdateRequest;
import com.segroup8.platform.vo.AddressVO;
import com.segroup8.platform.vo.MerchantApplicationVO;
import com.segroup8.platform.vo.UserVO;

import java.util.List;

public interface UserService {

    UserVO getCurrentUserProfile();

    List<UserVO> searchUsers(String keyword);

    void updateCurrentUserProfile(UserProfileUpdateRequest request);

    List<AddressVO> listMyAddresses();

    void createAddress(AddressSaveRequest request);

    void updateAddress(Long addressId, AddressSaveRequest request);

    void deleteAddress(Long addressId);

    void submitMerchantApplication(MerchantApplicationSubmitRequest request);

    MerchantApplicationVO getMyMerchantApplication();
}
