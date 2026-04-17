package com.cmbchina.orgperformance.config;

import com.cmbchina.orgperformance.service.ModelProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ModelProviderService modelProviderService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        createModelProviderTable();
        modelProviderService.initDefaultProviders();
    }

    private void createModelProviderTable() {
        try {
            String checkSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'model_provider'";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);

            if (count == null || count == 0) {
                logger.info("Creating model_provider table...");
                String createSql = """
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
                      `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
                      `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      INDEX `idx_code` (`code`),
                      INDEX `idx_status` (`status`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型服务商配置表'
                    """;
                jdbcTemplate.execute(createSql);
                logger.info("model_provider table created successfully");
            } else {
                logger.info("model_provider table already exists");
            }
        } catch (Exception e) {
            logger.error("Failed to create model_provider table: {}", e.getMessage());
        }
    }
}