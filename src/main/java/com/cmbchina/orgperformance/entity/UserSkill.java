package com.cmbchina.orgperformance.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserSkill {
    private Long id;
    private Long userId;
    private Long skillId;
    private Integer isInstalled;
    private LocalDateTime installedAt;
}
