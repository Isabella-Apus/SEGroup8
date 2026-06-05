package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.entity.Notification;
import com.segroup8.platform.mapper.NotificationMapper;
import com.segroup8.platform.realtime.RealtimePushService;
import com.segroup8.platform.service.NotificationService;
import com.segroup8.platform.vo.NotificationVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final RealtimePushService realtimePushService;

    public NotificationServiceImpl(NotificationMapper notificationMapper, RealtimePushService realtimePushService) {
        this.notificationMapper = notificationMapper;
        this.realtimePushService = realtimePushService;
    }

    @Override
    public List<NotificationVO> listMyNotifications(Long userId, String scope) {
        requireUserId(userId);
        String normalizedScope = normalizeScope(scope);
        return notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByAsc(Notification::getIsRead)
                        .orderByDesc(Notification::getCreateTime)
                        .orderByDesc(Notification::getId))
                .stream()
                .map(this::toVO)
                .filter(notification -> normalizedScope == null || normalizedScope.equals(notification.getScope()))
                .toList();
    }

    @Override
    public void markRead(Long userId, Long notificationId) {
        requireUserId(userId);
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !userId.equals(notification.getUserId())) {
            throw new BusinessException(404, "通知不存在");
        }
        if (Integer.valueOf(1).equals(notification.getIsRead())) {
            return;
        }
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .set(Notification::getIsRead, 1)
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId));
    }

    @Override
    public void markAllRead(Long userId, String scope) {
        requireUserId(userId);
        String normalizedScope = normalizeScope(scope);
        if (normalizedScope == null) {
            notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                    .set(Notification::getIsRead, 1)
                    .eq(Notification::getUserId, userId)
                    .eq(Notification::getIsRead, 0));
            return;
        }
        List<Long> ids = notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0))
                .stream()
                .map(this::toVO)
                .filter(notification -> normalizedScope.equals(notification.getScope()))
                .map(NotificationVO::getId)
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .set(Notification::getIsRead, 1)
                .eq(Notification::getUserId, userId)
                .in(Notification::getId, ids));
    }

    @Override
    public NotificationVO createNotification(Long userId, String title, String content) {
        return createNotification(userId, title, content, null);
    }

    @Override
    public NotificationVO createNotification(Long userId, String title, String content, String targetPath) {
        requireUserId(userId);
        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            throw new BusinessException(400, "通知内容不能为空");
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title.trim());
        notification.setContent(content.trim());
        if (StringUtils.hasText(targetPath)) {
            notification.setTargetPath(targetPath.trim());
        }
        notification.setIsRead(0);
        notification.setCreateTime(java.time.LocalDateTime.now());
        notificationMapper.insert(notification);
        NotificationVO vo = toVO(notification);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", vo.getId());
        payload.put("title", vo.getTitle());
        payload.put("content", vo.getContent());
        payload.put("targetPath", vo.getTargetPath());
        payload.put("scope", vo.getScope());
        payload.put("isRead", vo.getIsRead());
        payload.put("createTime", vo.getCreateTime());
        realtimePushService.pushToUser(userId, "NOTIFICATION_CREATED", payload);
        return vo;
    }

    private void requireUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
    }

    private NotificationVO toVO(Notification notification) {
        NotificationVO vo = new NotificationVO();
        vo.setId(notification.getId());
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setTargetPath(notification.getTargetPath());
        vo.setScope(inferScope(notification));
        vo.setIsRead(notification.getIsRead());
        vo.setCreateTime(notification.getCreateTime());
        return vo;
    }

    private String normalizeScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            return null;
        }
        String value = scope.trim().toLowerCase();
        if ("buyer".equals(value) || "seller".equals(value)) {
            return value;
        }
        return null;
    }

    private String inferScope(Notification notification) {
        String text = ((notification.getTitle() == null ? "" : notification.getTitle()) + " "
                + (notification.getContent() == null ? "" : notification.getContent()));
        if (text.contains("发货")
                || text.contains("卖家")
                || text.contains("店铺")
                || text.contains("工作台")
                || text.contains("商品审核")
                || text.contains("商品需要修改")
                || text.contains("商品审核未通过")
                || (text.contains("入驻") && text.contains("通过"))) {
            return "seller";
        }
        return "buyer";
    }
}
