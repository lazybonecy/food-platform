package com.food.article.service;

import com.food.article.dto.NotificationDTO;

import java.util.List;

public interface NotificationService {

    void createNotification(Long userId, Long fromUserId, Integer type, Long articleId, Long commentId, String content);

    List<NotificationDTO> listNotifications(Long userId);

    int countUnread(Long userId);

    void markAsRead(Long userId, Long notificationId);

    void markAllAsRead(Long userId);
}
