package com.cmbchina.orgperformance.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SystemCreateRequest {
    private String name;
    private String description;
    private Boolean needApproval;
    private List<InstitutionDTO> institutions;
    private List<IndicatorDTO> indicators;

    @Data
    public static class InstitutionDTO {
        private String orgName;
        private String orgId;
        private String groupName;
        private String leaderName;
        private String leaderEmpNo;
    }

    @Data
    public static class IndicatorDTO {
        private String dimension;
        private String category;
        private String level1Name;
        private String level2Name;
        private Double weight;
        private String unit;
        private Integer rowIndex;
    }
}
