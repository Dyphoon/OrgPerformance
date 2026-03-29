package com.cmbchina.termgoal.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OverviewVO {
    private Long monitoringId;
    private String systemName;
    private Integer year;
    private Integer month;
    private List<InstitutionRank> institutionRanks;
    private List<String> dimensions;
    private List<GroupOverview> groupOverviews;

    @Data
    public static class InstitutionRank {
        private Long institutionId;
        private String institutionName;
        private String groupName;
        private BigDecimal totalScore;
        private Integer rank;
        private Integer groupRank;
    }

    @Data
    public static class GroupOverview {
        private String groupName;
        private BigDecimal avgScore;
        private List<InstitutionRank> institutions;
    }
}
