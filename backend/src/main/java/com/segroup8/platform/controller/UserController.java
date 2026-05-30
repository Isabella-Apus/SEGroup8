package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.dto.AddressSaveRequest;
import com.segroup8.platform.dto.MerchantApplicationSubmitRequest;
import com.segroup8.platform.dto.UserProfileUpdateRequest;
import com.segroup8.platform.service.UserService;
import com.segroup8.platform.vo.AddressVO;
import com.segroup8.platform.vo.MerchantApplicationVO;
import com.segroup8.platform.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public Result<UserVO> profile() {
        return Result.success(userService.getCurrentUserProfile());
    }

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(userService.getCurrentUserProfile());
    }

    @GetMapping("/search")
    public Result<List<UserVO>> searchUsers(@RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(userService.searchUsers(keyword));
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        userService.updateCurrentUserProfile(request);
        return Result.success();
    }

    @GetMapping("/addresses")
    public Result<List<AddressVO>> listAddresses() {
        return Result.success(userService.listMyAddresses());
    }

    @PostMapping("/addresses")
    public Result<Void> createAddress(@Valid @RequestBody AddressSaveRequest request) {
        userService.createAddress(request);
        return Result.success();
    }

    @PutMapping("/addresses/{addressId}")
    public Result<Void> updateAddress(@PathVariable Long addressId, @Valid @RequestBody AddressSaveRequest request) {
        userService.updateAddress(addressId, request);
        return Result.success();
    }

    @DeleteMapping("/addresses/{addressId}")
    public Result<Void> deleteAddress(@PathVariable Long addressId) {
        userService.deleteAddress(addressId);
        return Result.success();
    }

    @PostMapping("/merchant-application")
    public Result<Void> submitMerchantApplication(@Valid @RequestBody MerchantApplicationSubmitRequest request) {
        userService.submitMerchantApplication(request);
        return Result.success();
    }

    @GetMapping({ "/merchant-application/me", "/merchant-application/me." })
    public Result<MerchantApplicationVO> getMyMerchantApplication() {
        return Result.success(userService.getMyMerchantApplication());
    }
}
