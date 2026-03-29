package com.cmbchina.orgperformance.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InstitutionLeader {
    private Long id;
    private Long institutionId;
    private Long userId;
    private Boolean confirmed;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
}
