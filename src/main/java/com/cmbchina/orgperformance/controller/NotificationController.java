package com.cmbchina.orgperformance.controller;

import com.cmbchina.orgperformance.entity.Notification;
import com.cmbchina.orgperformance.notify.NotificationService;
import com.cmbchina.orgperformance.vo.ApiResponse;
import com.cmbchina.orgperformance.vo.NotificationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationVO>> getNotifications(@RequestParam Long userId) {
        List<Notification> notifications = notificationService.getUserNotifications(userId);
        List<NotificationVO> voList = notifications.stream().map(n -> {
            NotificationVO vo = new NotificationVO();
            vo.setId(n.getId());
            vo.setTitle(n.getTitle());
            vo.setContent(n.getContent());
            vo.setType(n.getType());
            vo.setStatus(n.getStatus());
            vo.setCreatedAt(n.getCreatedAt());
            vo.setIsRead(Notification.STATUS_SENT.equals(n.getStatus()));
            return vo;
        }).collect(Collectors.toList());
        return ApiResponse.success(voList);
    }

    @GetMapping("/unread-count")
    public ApiResponse<Integer> getUnreadCount(@RequestParam Long userId) {
        int count = notificationService.getUnreadCount(userId);
        return ApiResponse.success(count);
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ApiResponse.success();
    }
}
