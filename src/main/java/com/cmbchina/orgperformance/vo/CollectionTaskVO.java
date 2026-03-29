package com.cmbchina.orgperformance.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CollectionTaskVO {
    private Long id;
    private Long monitoringId;
    private Long indicatorId;
    private Long institutionId;
    private String institutionName;
    private String dimension;
    private String category;
    private String level1Name;
    private String level2Name;
    private String unit;
    private BigDecimal annualTarget;
    private BigDecimal progressTarget;

    // 数据收集页的收数指标信息
    private String collectionIndicatorName; // 收数指标名称
    private String collectionUnit;          // 收数单位
    private String collectionDimension;    // 维度
    private String collectionCategory;     // 分类

    private String collectorName;
    private String collectorEmpNo;
    private Long collectorUserId;
    private BigDecimal actualValue;
    private String status;
    private String fileKey;
    private List<CollectionTaskVO> tasks;
}
