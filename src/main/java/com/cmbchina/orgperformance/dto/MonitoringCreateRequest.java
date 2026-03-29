package com.cmbchina.orgperformance.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MonitoringCreateRequest {
    private Long systemId;
    private Integer year;
    private Integer month;
    private LocalDateTime deadline;
    private Boolean approvalRequired;
}
