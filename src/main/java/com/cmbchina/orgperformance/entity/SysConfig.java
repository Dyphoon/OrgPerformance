package com.cmbchina.orgperformance.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysConfig {
    private Long id;
    private String configKey;
    private String configValue;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
