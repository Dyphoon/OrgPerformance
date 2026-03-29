package com.cmbchina.termgoal.vo;

import lombok.Data;

@Data
public class ConfirmationTaskVO {
    private Long id;
    private Long monitoringId;
    private Long institutionId;
    private String institutionName;
    private Long userId;
    private String leaderName;
    private String status;
    private String remark;
    private String confirmedAt;
}
