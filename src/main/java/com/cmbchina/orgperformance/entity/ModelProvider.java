package com.cmbchina.orgperformance.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ModelProvider {
    private Long id;
    private String name;
    private String code;
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private String modelType;
    private Integer maxTokens;
    private Double temperature;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}