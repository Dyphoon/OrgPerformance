package com.cmbchina.termgoal.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AssessmentSystem {
    private Long id;
    private String name;
    private String description;
    private String templateFileKey;
    private Boolean needApproval;
    private Integer status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
