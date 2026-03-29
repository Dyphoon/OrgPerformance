package com.cmbchina.termgoal.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ReportVO {
    private Long monitoringId;
    private String systemName;
    private Integer year;
    private Integer month;
    private Long institutionId;
    private String institutionName;
    private String groupName;
    private BigDecimal totalScore;
    private Integer totalRank;
    private Integer groupRank;
    private List<DimensionScore> dimensionScores;
    private List<IndicatorData> indicators;
    private Map<String, Object> charts;

    @Data
    public static class DimensionScore {
        private String dimension;
        private BigDecimal score;
        private BigDecimal weight;
    }

    @Data
    public static class IndicatorData {
        private String dimension;
        private String category;
        private String level1Name;
        private String level2Name;
        private String unit;
        private BigDecimal actualValue;
        private BigDecimal target;
        private BigDecimal progressTarget;
        private BigDecimal completionRate;
        private BigDecimal score;
        private BigDecimal weight;
        private BigDecimal weightedScore;
    }
}
