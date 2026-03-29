package com.cmbchina.termgoal.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TaskUpdateRequest {
    private Long taskId;
    private BigDecimal actualValue;
}
