package com.cmbchina.orgperformance.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Skill {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private String category;
    private String prompt;
    private String tools;
    private Integer isBuiltIn;
    private Integer isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 技能详细说明(Markdown格式)
    private String markdownContent;
    // 脚本文件路径
    private String scriptPath;
    // 脚本内容
    private String scriptContent;
    // 版本号
    private String version;
    // 作者
    private String author;
}
