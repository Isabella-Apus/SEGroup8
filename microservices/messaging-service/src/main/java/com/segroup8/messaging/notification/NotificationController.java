package com.segroup8.messaging.notification;

import com.segroup8.messaging.common.ApiResult;
import com.segroup8.messaging.security.JwtAuthenticationInterceptor;
import com.segroup8.messaging.security.MessagingPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }
    @GetMapping public ApiResult<List<NotificationService.NotificationView>> list(
            @RequestParam(required = false) String scope, HttpServletRequest request) {
        return ApiResult.success(service.list(principal(request).userId(), scope));
    }
    @PostMapping("/{id}/read") public ApiResult<Void> read(@PathVariable long id, HttpServletRequest request) {
        service.markRead(principal(request).userId(), id); return ApiResult.success();
    }
    @PostMapping("/read-all") public ApiResult<Void> readAll(
            @RequestParam(required = false) String scope, HttpServletRequest request) {
        service.markAllRead(principal(request).userId(), scope); return ApiResult.success();
    }
    private MessagingPrincipal principal(HttpServletRequest request) {
        return (MessagingPrincipal) request.getAttribute(JwtAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE);
    }
}
