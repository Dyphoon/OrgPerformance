package com.cmbchina.orgperformance.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MonitoringVO {
    private Long id;
    private Long systemId;
    private String systemName;
    private Integer year;
    private Integer month;
    private String period;
    private String status;
    private LocalDateTime deadline;
    private Boolean approvalRequired;
    private Integer processPercent;
    private String processStatus;
    private String processMsg;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer totalInstitutions;
    private Integer confirmedInstitutions;
    private Integer pendingInstitutions;
    private Integer totalTasks;
    private Integer submittedTasks;
    private Integer pendingTasks;
}
