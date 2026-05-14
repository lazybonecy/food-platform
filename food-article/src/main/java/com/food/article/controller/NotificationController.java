package com.food.article.controller;

import com.food.article.dto.NotificationDTO;
import com.food.article.service.NotificationService;
import com.food.common.core.constant.CommonConstant;
import com.food.common.core.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/list")
    public R<List<NotificationDTO>> listNotifications(@RequestHeader(CommonConstant.USER_ID_HEADER) Long userId) {
        return R.ok(notificationService.listNotifications(userId));
    }

    @GetMapping("/unread-count")
    public R<Map<String, Integer>> countUnread(@RequestHeader(CommonConstant.USER_ID_HEADER) Long userId) {
        Map<String, Integer> map = new HashMap<>();
        map.put("count", notificationService.countUnread(userId));
        return R.ok(map);
    }

    @PutMapping("/{id}/read")
    public R<Void> markAsRead(@RequestHeader(CommonConstant.USER_ID_HEADER) Long userId,
                              @PathVariable Long id) {
        notificationService.markAsRead(userId, id);
        return R.ok();
    }

    @PutMapping("/read-all")
    public R<Void> markAllAsRead(@RequestHeader(CommonConstant.USER_ID_HEADER) Long userId) {
        notificationService.markAllAsRead(userId);
        return R.ok();
    }
}
