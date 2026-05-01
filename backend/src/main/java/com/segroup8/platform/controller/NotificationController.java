package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.service.NotificationService;
import com.segroup8.platform.vo.NotificationVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Result<List<NotificationVO>> listMyNotifications() {
        return Result.success(notificationService.listMyNotifications(UserContext.getUserId()));
    }

    @PostMapping("/{notificationId}/read")
    public Result<Void> markRead(@PathVariable Long notificationId) {
        notificationService.markRead(UserContext.getUserId(), notificationId);
        return Result.success();
    }

    @PostMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead(UserContext.getUserId());
        return Result.success();
    }
}
