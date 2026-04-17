CREATE TABLE IF NOT EXISTS `model_provider` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL COMMENT '服务商名称',
  `code` VARCHAR(50) NOT NULL UNIQUE COMMENT '服务商代码',
  `base_url` VARCHAR(500) NOT NULL COMMENT 'API地址',
  `api_key` VARCHAR(500) NOT NULL COMMENT 'API密钥',
  `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
  `model_type` VARCHAR(50) NOT NULL COMMENT '模型类型',
  `max_tokens` INT DEFAULT 4096 COMMENT '最大Token数',
  `temperature` DECIMAL(3,2) DEFAULT 0.70 COMMENT '温度参数',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` INT DEFAULT 1 COMMENT '状态：1启用，0停用',
  `is_default` TINYINT(1) DEFAULT 0 COMMENT '是否默认',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_code` (`code`),
  INDEX `idx_status` (`status`),
  INDEX `idx_is_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型服务商配置表';

-- 初始化默认的 MiniMax 和智谱 GLM 配置
INSERT INTO `model_provider` (`name`, `code`, `base_url`, `api_key`, `model_name`, `model_type`, `max_tokens`, `temperature`, `sort_order`, `status`, `is_default`) VALUES
('MiniMax', 'minimax', 'https://api.minimaxi.com/v1', '', 'MiniMax-M2.7', 'minimax', 196608, 0.70, 1, 1, 1),
('智谱 GLM', 'glm', 'https://open.bigmodel.cn/api/paas/v4', '', 'glm-4', 'glm', 128000, 0.70, 2, 1, 0);
