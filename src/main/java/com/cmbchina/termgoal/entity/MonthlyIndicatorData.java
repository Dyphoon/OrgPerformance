package com.cmbchina.termgoal.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MonthlyIndicatorData {
    private Long id;
    private Long monitoringId;
    private Long indicatorId;
    private Long institutionId;
    private BigDecimal actualValue;
    private BigDecimal annualCompletionRate;
    private BigDecimal progressCompletionRate;
    private BigDecimal score100;
    private BigDecimal scoreWeighted;
    private BigDecimal scoreCategory;
    private BigDecimal scoreDimension;
    private BigDecimal totalScore;
    private String fileKey;
    private LocalDateTime createdAt;
}
