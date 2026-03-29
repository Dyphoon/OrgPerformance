package com.cmbchina.termgoal.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MonthlyMonitoring {
    private Long id;
    private Long systemId;
    private Integer year;
    private Integer month;
    private String status;
    private LocalDateTime deadline;
    private Boolean approvalRequired;
    private Integer processPercent;
    private String processStatus;
    private String processMsg;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COLLECTING = "COLLECTING";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_CONFIRMING = "CONFIRMING";
    public static final String STATUS_PUBLISHED = "PUBLISHED";

    public static final String PROCESS_IDLE = "idle";
    public static final String PROCESS_PROCESSING = "processing";
    public static final String PROCESS_DONE = "done";
    public static final String PROCESS_FAILED = "failed";
}
