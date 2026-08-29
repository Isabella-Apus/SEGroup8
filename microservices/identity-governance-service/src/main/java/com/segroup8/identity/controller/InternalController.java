package com.segroup8.identity.controller;

import com.segroup8.identity.api.ApiResult;
import com.segroup8.identity.service.IdentityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal")
public class InternalController {
    private final IdentityService service;

    public InternalController(IdentityService service) {
        this.service = service;
    }

    @PostMapping("/auth/introspect")
    public ApiResult<Map<String, Object>> introspect(@RequestBody Map<String, Object> request) {
        return ApiResult.success(service.introspect(String.valueOf(request.getOrDefault("token", ""))));
    }

    @GetMapping("/users/{id}/summary")
    public ApiResult<Map<String, Object>> summary(@PathVariable long id) {
        return ApiResult.success(service.userSummary(id));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/blocks/check")
    public ApiResult<List<Map<String, Object>>> check(@RequestBody Map<String, Object> request) {
        Object pairs = request.get("pairs");
        return ApiResult.success(service.checkBlocks(pairs instanceof List<?> list
                ? (List<Map<String, Object>>) (List<?>) list : List.of()));
    }
}
