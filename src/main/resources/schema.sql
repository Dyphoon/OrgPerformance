-- 数据库初始化脚本
CREATE DATABASE IF NOT EXISTS termgoal DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE termgoal;

-- 报表文件表
CREATE TABLE IF NOT EXISTS report_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_size BIGINT NOT NULL COMMENT '文件大小',
    file_type VARCHAR(100) COMMENT '文件类型',
    minio_url VARCHAR(500) COMMENT 'MinIO存储URL',
    minio_object_name VARCHAR(255) COMMENT 'MinIO对象名',
    description TEXT COMMENT '描述',
    status INT NOT NULL DEFAULT 0 COMMENT '状态: 0-待处理 1-处理中 2-已完成 3-失败',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表文件表';

-- 报表数据表
CREATE TABLE IF NOT EXISTS report_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_file_id BIGINT NOT NULL COMMENT '报表文件ID',
    sheet_name VARCHAR(100) COMMENT '工作表名称',
    row_num INT COMMENT '行号',
    category VARCHAR(100) COMMENT '类别',
    item_name VARCHAR(200) COMMENT '指标名称',
    item_code VARCHAR(50) COMMENT '指标编码',
    target_value DECIMAL(20,4) COMMENT '目标值',
    actual_value DECIMAL(20,4) COMMENT '实际值',
    completion_rate DECIMAL(10,4) COMMENT '完成率(%)',
    unit VARCHAR(20) COMMENT '单位',
    department VARCHAR(100) COMMENT '部门',
    period VARCHAR(50) COMMENT '周期',
    report_date DATE COMMENT '报表日期',
    raw_data TEXT COMMENT '原始数据JSON',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_report_file_id (report_file_id),
    INDEX idx_category (category),
    INDEX idx_department (department),
    INDEX idx_period (period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表数据表';
