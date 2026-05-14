package com.food.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.food.article.dto.NotificationDTO;
import com.food.article.entity.Article;
import com.food.article.entity.Notification;
import com.food.article.mapper.ArticleMapper;
import com.food.article.mapper.NotificationMapper;
import com.food.article.service.NotificationService;
import com.food.article.ws.NotificationWebSocketHandler;
import com.food.api.user.UserDubboService;
import com.food.api.user.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final ArticleMapper articleMapper;
    private final NotificationWebSocketHandler wsHandler;

    @DubboReference
    private UserDubboService userDubboService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Map<Integer, String> TYPE_DESC_MAP = new HashMap<>();
    static {
        TYPE_DESC_MAP.put(1, "评论了你的文章");
        TYPE_DESC_MAP.put(2, "回复了你的评论");
        TYPE_DESC_MAP.put(3, "给你发了一条私信");
    }

    @Override
    public void createNotification(Long userId, Long fromUserId, Integer type, Long articleId, Long commentId, String content) {
        // 不给自己发通知
        if (userId.equals(fromUserId)) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setFromUserId(fromUserId);
        notification.setType(type);
        notification.setArticleId(articleId);
        notification.setCommentId(commentId);
        notification.setContent(content);
        notification.setIsRead(0);
        notificationMapper.insert(notification);

        // 通过 WebSocket 实时推送给目标用户
        try {
            String fromNickname = "用户";
            try {
                UserDTO user = userDubboService.getUserById(fromUserId);
                if (user != null && user.getNickname() != null && !user.getNickname().isEmpty()) {
                    fromNickname = user.getNickname();
                }
            } catch (Exception ignored) {}

            String articleTitle = "";
            if (articleId != null) {
                Article article = articleMapper.selectById(articleId);
                if (article != null) articleTitle = article.getTitle();
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "notification");
            payload.put("id", notification.getId());
            payload.put("fromNickname", fromNickname);
            payload.put("notifyType", type);
            payload.put("typeDesc", TYPE_DESC_MAP.getOrDefault(type, "通知"));
            payload.put("articleId", articleId);
            payload.put("articleTitle", articleTitle);
            payload.put("commentId", commentId);
            payload.put("content", content);

            wsHandler.sendToUser(userId, payload);
        } catch (Exception e) {
            log.warn("Failed to push WebSocket notification: {}", e.getMessage());
        }
    }

    @Override
    public List<NotificationDTO> listNotifications(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreateTime);
        List<Notification> notifications = notificationMapper.selectList(wrapper);

        if (notifications.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量获取发送者昵称
        Set<Long> fromUserIds = notifications.stream()
                .map(Notification::getFromUserId)
                .collect(Collectors.toSet());
        Map<Long, String> nicknameMap = batchGetNicknames(fromUserIds);

        // 批量获取文章标题
        Set<Long> articleIds = notifications.stream()
                .map(Notification::getArticleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> articleTitleMap = batchGetArticleTitles(articleIds);

        return notifications.stream().map(n -> {
            NotificationDTO dto = new NotificationDTO();
            dto.setId(n.getId());
            dto.setFromNickname(nicknameMap.getOrDefault(n.getFromUserId(), "用户"));
            dto.setType(n.getType());
            dto.setTypeDesc(TYPE_DESC_MAP.getOrDefault(n.getType(), "通知"));
            dto.setArticleId(n.getArticleId());
            dto.setArticleTitle(articleTitleMap.getOrDefault(n.getArticleId(), ""));
            dto.setCommentId(n.getCommentId());
            dto.setContent(n.getContent());
            dto.setIsRead(n.getIsRead());
            if (n.getCreateTime() != null) {
                dto.setCreateTime(n.getCreateTime().format(FORMATTER));
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public int countUnread(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0);
        return Math.toIntExact(notificationMapper.selectCount(wrapper));
    }

    @Override
    public void markAsRead(Long userId, Long notificationId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1);
        notificationMapper.update(null, wrapper);
    }

    @Override
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1);
        notificationMapper.update(null, wrapper);
    }

    private Map<Long, String> batchGetNicknames(Set<Long> userIds) {
        Map<Long, String> map = new HashMap<>();
        for (Long uid : userIds) {
            try {
                UserDTO user = userDubboService.getUserById(uid);
                if (user != null && user.getNickname() != null && !user.getNickname().isEmpty()) {
                    map.put(uid, user.getNickname());
                } else {
                    map.put(uid, "用户");
                }
            } catch (Exception e) {
                log.warn("Failed to get user nickname for userId={}: {}", uid, e.getMessage());
                map.put(uid, "用户");
            }
        }
        return map;
    }

    private Map<Long, String> batchGetArticleTitles(Set<Long> articleIds) {
        Map<Long, String> map = new HashMap<>();
        if (articleIds.isEmpty()) return map;
        List<Article> articles = articleMapper.selectBatchIds(articleIds);
        for (Article a : articles) {
            map.put(a.getId(), a.getTitle());
        }
        return map;
    }
}
