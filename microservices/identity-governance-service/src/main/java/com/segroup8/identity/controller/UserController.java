package com.segroup8.identity.controller;

import com.segroup8.identity.api.ApiResult;
import com.segroup8.identity.service.IdentityService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final IdentityService service;

    public UserController(IdentityService service) {
        this.service = service;
    }

    @GetMapping({"/profile", "/me"})
    public ApiResult<Map<String, Object>> profile() {
        return ApiResult.success(service.profile());
    }

    @PutMapping("/profile")
    public ApiResult<Void> updateProfile(@RequestBody Map<String, Object> request) {
        service.updateProfile(text(request, "nickname"), text(request, "avatar"),
                text(request, "phone"), text(request, "email"));
        return ApiResult.success();
    }

    @GetMapping("/search")
    public ApiResult<List<Map<String, Object>>> search(@RequestParam(defaultValue = "") String keyword) {
        return ApiResult.success(service.searchUsers(keyword));
    }

    @GetMapping("/addresses")
    public ApiResult<List<Map<String, Object>>> addresses() {
        return ApiResult.success(service.addresses());
    }

    @PostMapping("/addresses")
    public ApiResult<Void> addAddress(@RequestBody Map<String, Object> request) {
        service.addAddress(request);
        return ApiResult.success();
    }

    @PutMapping("/addresses/{id}")
    public ApiResult<Void> updateAddress(@PathVariable long id, @RequestBody Map<String, Object> request) {
        service.updateAddress(id, request);
        return ApiResult.success();
    }

    @DeleteMapping("/addresses/{id}")
    public ApiResult<Void> deleteAddress(@PathVariable long id) {
        service.deleteAddress(id);
        return ApiResult.success();
    }

    @PostMapping("/merchant-application")
    public ApiResult<Void> submitMerchant(@RequestBody Map<String, Object> request) {
        service.submitMerchantApplication(request);
        return ApiResult.success();
    }

    @GetMapping("/merchant-application/me")
    public ApiResult<Map<String, Object>> myMerchant() {
        return ApiResult.success(service.myMerchantApplication());
    }

    private String text(Map<String, Object> request, String key) {
        return request.get(key) == null ? null : String.valueOf(request.get(key));
    }
}
