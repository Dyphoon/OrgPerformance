package com.cmbchina.termgoal.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Notification {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String type;
    private String status;
    private String sendResult;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;

    public static final String TYPE_IM = "im";
    public static final String TYPE_EMAIL = "email";
    public static final String TYPE_SITE = "site";

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_SENT = "sent";
    public static final String STATUS_FAILED = "failed";
}
