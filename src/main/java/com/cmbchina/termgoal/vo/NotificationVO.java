package com.cmbchina.termgoal.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationVO {
    private Long id;
    private String title;
    private String content;
    private String type;
    private String status;
    private LocalDateTime createdAt;
    private Boolean isRead;
}
