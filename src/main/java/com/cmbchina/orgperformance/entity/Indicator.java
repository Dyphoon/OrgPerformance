package com.cmbchina.orgperformance.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Indicator {
    private Long id;
    private Long systemId;
    private String dimension;
    private String category;
    private String level1Name;
    private String level2Name;
    private BigDecimal weight;
    private String unit;
    private Integer rowIndex;
    private LocalDateTime createdAt;
}
