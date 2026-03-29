package com.cmbchina.orgperformance.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SystemVO {
    private Long id;
    private String name;
    private String description;
    private String templateFileKey;
    private Boolean needApproval;
    private Integer status;
    private String createdBy;
    private LocalDateTime createdAt;
    private Integer institutionCount;
    private Integer indicatorCount;
    private List<String> groupNames;
}
