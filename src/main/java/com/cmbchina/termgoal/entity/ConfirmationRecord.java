package com.cmbchina.termgoal.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConfirmationRecord {
    private Long id;
    private Long monitoringId;
    private Long institutionId;
    private Long userId;
    private String status;
    private LocalDateTime confirmedAt;
    private String remark;
    private LocalDateTime createdAt;

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_CONFIRMED = "confirmed";
}
