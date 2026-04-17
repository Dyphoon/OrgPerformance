-- 技能表
CREATE TABLE IF NOT EXISTS skill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '技能名称',
    description VARCHAR(500) COMMENT '技能描述',
    icon VARCHAR(50) COMMENT '图标',
    category VARCHAR(50) COMMENT '分类',
    prompt TEXT COMMENT '技能提示词',
    tools VARCHAR(500) COMMENT '关联的工具列表',
    is_built_in INT NOT NULL DEFAULT 0 COMMENT '是否内置',
    is_active INT NOT NULL DEFAULT 1 COMMENT '是否激活',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    markdown_content TEXT COMMENT '技能详细说明(Markdown)',
    script_path VARCHAR(255) COMMENT '脚本文件路径',
    script_content TEXT COMMENT '脚本内容',
    version VARCHAR(20) COMMENT '版本号',
    author VARCHAR(100) COMMENT '作者',
    INDEX idx_category (category),
    INDEX idx_is_built_in (is_built_in)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='技能表';

-- 用户技能安装表
CREATE TABLE IF NOT EXISTS user_skill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    skill_id BIGINT NOT NULL COMMENT '技能ID',
    is_installed INT NOT NULL DEFAULT 1 COMMENT '是否已安装',
    installed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '安装时间',
    INDEX idx_user_id (user_id),
    INDEX idx_skill_id (skill_id),
    UNIQUE KEY uk_user_skill (user_id, skill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户技能安装表';
