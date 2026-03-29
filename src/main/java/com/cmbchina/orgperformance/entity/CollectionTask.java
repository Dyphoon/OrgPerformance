package com.cmbchina.orgperformance.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CollectionTask {
    private Long id;
    private Long monitoringId;
    private Long indicatorId;
    private Long institutionId;
    private String collectorName;
    private String collectorEmpNo;
    private Long collectorUserId;
    private BigDecimal actualValue;
    private String status;
    private LocalDateTime submittedAt;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String remark;
    private LocalDateTime createdAt;
    private String fileKey; // 收数文档的MinIO路径

    // 数据收集页的收数指标信息
    private String collectionIndicatorName; // 收数指标名称（如"日均存款余额"）
    private String collectionUnit;          // 收数单位
    private String collectionDimension;    // 维度（从数据收集页获取）
    private String collectionCategory;      // 分类（从数据收集页获取）

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_SUBMITTED = "submitted";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";
}
