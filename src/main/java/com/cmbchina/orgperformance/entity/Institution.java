package com.cmbchina.orgperformance.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Institution {
    private Long id;
    private Long systemId;
    private String orgName;
    private String orgId;
    private String groupName;
    private String leaderName;
    private String leaderEmpNo;
    private LocalDateTime createdAt;
}
