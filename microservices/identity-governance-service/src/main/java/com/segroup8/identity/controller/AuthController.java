package com.segroup8.identity.controller;

import com.segroup8.identity.api.ApiResult;
import com.segroup8.identity.service.IdentityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final IdentityService service;

    public AuthController(IdentityService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ApiResult<Void> register(@Valid @RequestBody RegisterRequest request) {
        service.register(request.username(), request.password(), request.nickname(), request.phone(), request.email());
        return ApiResult.success();
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResult.success(service.login(request.username(), request.password()));
    }

    public record RegisterRequest(@NotBlank @Size(max = 50) String username,
            @NotBlank @Size(min = 6, max = 32) String password,
            @NotBlank @Size(max = 50) String nickname, String phone, String email) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }
}
