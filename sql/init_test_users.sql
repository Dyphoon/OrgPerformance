-- =====================================================
-- 测试数据脚本：收数人和机构负责人
-- =====================================================

USE orgperformance;

-- 1. 确保角色存在
INSERT IGNORE INTO sys_role (id, role_code, role_name, description, created_at) VALUES 
(2, 'collector', '收数员', '负责收集绩效数据', NOW()),
(3, 'leader', '机构负责人', '负责确认本机构绩效数据', NOW());

-- 2. 创建收数员用户 (密码都是 admin123 的 BCrypt 哈希)
INSERT IGNORE INTO sys_user (id, username, password, name, emp_no, email, phone, status, created_at, updated_at) VALUES
(2, 'collector1', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '张三', 'C001', 'zhangsan@cmbc.com', '13800001001', 1, NOW(), NOW()),
(3, 'collector2', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '李四', 'C002', 'lisi@cmbc.com', '13800001002', 1, NOW(), NOW()),
(4, 'collector3', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '王五', 'C003', 'wangwu@cmbc.com', '13800001003', 1, NOW(), NOW());

-- 3. 给收数员分配角色 2
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES
(2, 2),
(3, 2),
(4, 2);

-- 4. 创建机构负责人用户 (密码都是 admin123)
INSERT IGNORE INTO sys_user (id, username, password, name, emp_no, email, phone, status, created_at, updated_at) VALUES
(5, 'leader1', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '赵六', 'L001', 'zhaoliu@cmbc.com', '13800001005', 1, NOW(), NOW()),
(6, 'leader2', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '钱七', 'L002', 'qianqi@cmbc.com', '13800001006', 1, NOW(), NOW()),
(7, 'leader3', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '孙八', 'L003', 'sunba@cmbc.com', '13800001007', 1, NOW(), NOW());

-- 5. 给机构负责人分配角色 3
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES
(5, 3),
(6, 3),
(7, 3);

-- 6. 机构负责人与机构关联
-- 注意：需要先有机构数据才能关联，这里先创建一个示例机构
INSERT IGNORE INTO institution (id, system_id, org_name, org_id, group_name, leader_name, leader_emp_no, created_at) VALUES
(1, 1, '北京分行', 'BJ001', '北方区', '赵六', 'L001', NOW()),
(2, 1, '上海分行', 'SH001', '南方区', '钱七', 'L002', NOW()),
(3, 1, '深圳分行', 'SZ001', '南方区', '孙八', 'L003', NOW());

-- 7. 将机构负责人关联到机构
INSERT IGNORE INTO institution_leader (institution_id, user_id, confirmed, created_at) VALUES
(1, 5, 0, NOW()),
(2, 6, 0, NOW()),
(3, 7, 0, NOW());

SELECT '测试数据创建完成！' AS message;
SELECT '收数员账号：collector1, collector2, collector3 (密码: admin123)' AS collector_info;
SELECT '机构负责人账号：leader1, leader2, leader3 (密码: admin123)' AS leader_info;
