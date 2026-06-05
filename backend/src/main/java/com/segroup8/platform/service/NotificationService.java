package com.segroup8.platform.service;

import com.segroup8.platform.vo.NotificationVO;

import java.util.List;

public interface NotificationService {

    List<NotificationVO> listMyNotifications(Long userId, String scope);

    void markRead(Long userId, Long notificationId);

    void markAllRead(Long userId, String scope);

    NotificationVO createNotification(Long userId, String title, String content);

    NotificationVO createNotification(Long userId, String title, String content, String targetPath);
}
