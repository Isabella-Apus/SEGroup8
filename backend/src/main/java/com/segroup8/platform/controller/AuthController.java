package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.dto.LoginRequest;
import com.segroup8.platform.dto.RegisterRequest;
import com.segroup8.platform.service.AuthService;
import com.segroup8.platform.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "用户注册")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = RegisterRequest.class), examples = @ExampleObject(value = "{\n  \"username\": \"new_user\",\n  \"password\": \"newPass123\",\n  \"nickname\": \"新人用户\",\n  \"phone\": \"13800138000\",\n  \"email\": \"new@demo.com\"\n}")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "注册成功", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":0,\"message\":\"success\",\"data\":null}"))),
            @ApiResponse(responseCode = "400", description = "参数错误或用户名重复", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":400,\"message\":\"用户名已存在\",\"data\":null}")))
    })
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success();
    }

    @Operation(summary = "账号密码登录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginRequest.class), examples = @ExampleObject(value = "{\n  \"username\": \"user\",\n  \"password\": \"user123\"\n}")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":0,\"message\":\"success\",\"data\":{\"token\":\"jwt-token\",\"role\":\"USER\",\"user\":{\"id\":3,\"username\":\"user\",\"nickname\":\"DemoUser\"}}}"))),
            @ApiResponse(responseCode = "401", description = "账号或密码错误", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":401,\"message\":\"用户名或密码错误\",\"data\":null}")))
    })
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }
}
