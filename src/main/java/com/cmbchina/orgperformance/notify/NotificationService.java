package com.cmbchina.orgperformance.notify;

import com.cmbchina.orgperformance.entity.Notification;
import com.cmbchina.orgperformance.entity.SysUser;
import com.cmbchina.orgperformance.mapper.NotificationMapper;
import com.cmbchina.orgperformance.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@example.com}")
    private String fromEmail;

    @Value("${im.type:dingtalk}")
    private String imType;

    private final Map<String, NotifyStrategy> strategies = new HashMap<>();

    public NotificationService() {
        strategies.put("dingtalk", new DingTalkNotifyStrategy());
        strategies.put("feishu", new FeishuNotifyStrategy());
        strategies.put("wechatwork", new WechatWorkNotifyStrategy());
    }

    public void sendNotification(Long userId, String title, String content, String type) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) return;

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setStatus(Notification.STATUS_PENDING);
        notificationMapper.insert(notification);

        try {
            switch (type) {
                case Notification.TYPE_SITE:
                    notificationMapper.updateStatus(notification.getId(), Notification.STATUS_SENT);
                    break;
                case Notification.TYPE_EMAIL:
                    sendEmail(user, title, content);
                    notificationMapper.updateSendResult(notification.getId(), "sent");
                    break;
                case Notification.TYPE_IM:
                    NotifyStrategy strategy = strategies.get(imType);
                    if (strategy != null) {
                        String result = strategy.send(title, content, user.getPhone());
                        notificationMapper.updateSendResult(notification.getId(), result);
                    }
                    break;
            }
        } catch (Exception e) {
            notificationMapper.updateSendFailed(notification.getId(), e.getMessage());
        }
    }

    public void sendBatchNotifications(List<Long> userIds, String title, String content, String type) {
        for (Long userId : userIds) {
            sendNotification(userId, title, content, type);
        }
    }

    private void sendEmail(SysUser user, String title, String content) {
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new RuntimeException("User has no email configured");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(user.getEmail());
        message.setSubject(title);
        message.setText(content);
        mailSender.send(message);
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationMapper.selectByUserId(userId);
    }

    public int getUnreadCount(Long userId) {
        return notificationMapper.countUnreadByUserId(userId);
    }

    public void markAsRead(Long notificationId) {
        notificationMapper.updateStatus(notificationId, Notification.STATUS_SENT);
    }

    public interface NotifyStrategy {
        String send(String title, String content, String phone);
    }

    public static class DingTalkNotifyStrategy implements NotifyStrategy {
        @Override
        public String send(String title, String content, String phone) {
            return "DingTalk notification sent";
        }
    }

    public static class FeishuNotifyStrategy implements NotifyStrategy {
        @Override
        public String send(String title, String content, String phone) {
            return "Feishu notification sent";
        }
    }

    public static class WechatWorkNotifyStrategy implements NotifyStrategy {
        @Override
        public String send(String title, String content, String phone) {
            return "WeChat Work notification sent";
        }
    }
}
