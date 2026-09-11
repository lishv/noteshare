-- 学生笔记共享平台数据库初始化脚本（包含测试数据）

CREATE DATABASE IF NOT EXISTS `note_sharing` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `note_sharing`;

-- 用户表
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名 (唯一)',
  `password` VARCHAR(255) NOT NULL COMMENT '密码 (BCrypt 加密)',
  `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
  `bio` VARCHAR(100) DEFAULT NULL COMMENT '个性签名 (最多30字)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-禁用，2-锁定',
  `last_active_at` DATETIME DEFAULT NULL COMMENT '最后活跃时间 (用于排序)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_last_active_at` (`last_active_at`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 总链接表
DROP TABLE IF EXISTS `t_main_link`;
CREATE TABLE `t_main_link` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `main_url` VARCHAR(500) NOT NULL COMMENT '总链接 URL',
  `view_count` BIGINT NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_updated_at` (`updated_at`),
  CONSTRAINT `fk_main_link_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='总链接表';

-- 笔记表
DROP TABLE IF EXISTS `t_note_item`;
CREATE TABLE `t_note_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '笔记 ID',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `title` VARCHAR(100) NOT NULL COMMENT '笔记名称',
  `location_or_url` VARCHAR(500) NOT NULL COMMENT '位置或链接',
  `view_count` BIGINT NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：1-已删除，0-未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id_updated_at` (`user_id`, `updated_at`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_updated_at` (`updated_at`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `fk_note_item_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记表';

-- 登录失败记录记录表
DROP TABLE IF EXISTS `t_login_fail_record`;
CREATE TABLE `t_login_fail_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `ip_address` VARCHAR(50) NOT NULL COMMENT 'IP 地址',
  `fail_count` INT NOT NULL DEFAULT 1 COMMENT '失败次数',
  `lock_until` DATETIME DEFAULT NULL COMMENT '锁定到期时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username_ip` (`username`, `ip_address`),
  KEY `idx_lock_until` (`lock_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录失败记录记录表';

-- 敏感词表
DROP TABLE IF EXISTS `t_sensitive_word`;
CREATE TABLE `t_sensitive_word` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `word` VARCHAR(100) NOT NULL COMMENT '敏感词',
  `type` TINYINT NOT NULL COMMENT '类型：1-政治，2-色情，3-暴力，4-广告',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_word` (`word`),
  KEY `idx_type_status` (`type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感词表';

-- ============================================
-- 插入测试数据
-- ============================================

-- 密码都是: 123456 (BCrypt加密后)
INSERT INTO `t_user` (`username`, `password`, `email`, `phone`, `avatar`, `status`, `last_active_at`) VALUES
('张三', '$2a$10$N.zmdr9k7uOCQb3X4lMlcO6wqZq5L5Y5G5q5L5Y5G5q5L5Y5G5q5L5Y5', 'zhangsan@example.com', '13800138001', 'https://api.dicebear.com/7.x/avataaars/svg?seed=张三', 1, NOW()),
('李四', '$2a$10$N.zmdr9k7uOCQb3X4lMlcO6wqZq5L5Y5G5q5L5Y5G5q5L5Y5G5q5L5Y5', 'lisi@example.com', '13800138002', 'https://api.dicebear.com/7.x/avataaars/svg?seed=李四', 1, NOW()),
('王五', '$2a$10$N.zmdr9k7uOCQb3X4lMlcO6wqZq5L5Y5G5q5L5Y5G5q5L5Y5G5q5L5Y5', 'wangwu@example.com', '13800138003', 'https://api.dicebear.com/7.x/avataaars/svg?seed=王五', 1, NOW()),
('赵六', '$2a$10$N.zmdr9k7uOCQb3X4lMlcO6wqZq5L5Y5G5q5L5Y5G5q5L5Y5G5q5L5Y5', 'zhaoliu@example.com', '13800138004', 'https://api.dicebear.com/7.x/avataaars/svg?seed=赵六', 1, NOW()),
('孙七', '$2a$10$N.zmdr9k7uOCQb3X4lMlcO6wqZq5L5Y5G5q5L5Y5G5q5L5Y5G5q5L5Y5', 'sunqi@example.com', '13800138005', 'https://api.dicebear.com/7.x/avataaars/svg?seed=孙七', 1, NOW());

-- 插入总链接数据
INSERT INTO `t_main_link` (`user_id`, `main_url`, `view_count`) VALUES
(1, 'https://notion.so/zhangsan-knowledge-base', 156),
(2, 'https://notion.so/lisi-study-notes', 89),
(3, 'https://notion.so/wangwu-learning', 234),
(4, 'https://notion.so/zhaoliu-notes', 67),
(5, 'https://notion.so/sunqi-knowledge', 112);

-- 插入笔记数据
INSERT INTO `t_note_item` (`user_id`, `title`, `location_or_url`, `view_count`) VALUES
-- 张三的笔记
(1, 'Java基础学习笔记', 'https://notion.so/zhangsan-knowledge-base/java-basics', 234),
(1, 'Spring Boot实战', 'https://notion.so/zhangsan-knowledge-base/spring-boot', 189),
(1, 'MySQL数据库优化', 'https://notion.so/zhangsan-knowledge-base/mysql-optimization', 156),
(1, 'Redis缓存策略', 'https://notion.so/zhangsan-knowledge-base/redis-strategy', 98),
(1, 'Vue3入门教程', 'https://notion.so/zhangsan-knowledge-base/vue3-tutorial', 167),

-- 李四的笔记
(2, 'Python数据分析', 'https://notion.so/lisi-study-notes/python-data-analysis', 145),
(2, 'Pandas使用指南', 'https://notion.so/lisi-study-notes/pandas-guide', 112),
(2, '机器学习入门', 'https://notion.so/lisi-study-notes/ml-intro', 87),
(2, '数据可视化', 'https://notion.so/lisi-study-notes/data-visualization', 65),
(2, 'Jupyter Notebook技巧', 'https://notion.so/lisi-study-notes/jupyter-tips', 54),

-- 王五的笔记
(3, '前端开发最佳实践', 'https://notion.so/wangwu-learning/frontend-best-practices', 198),
(3, 'CSS布局技巧', 'https://notion.so/wangwu-learning/css-layout', 134),
(3, 'JavaScript高级特性', 'https://notion.so/wangwu-learning/js-advanced', 167),
(3, 'TypeScript实战', 'https://notion.so/wangwu-learning/typescript', 123),
(3, 'Webpack配置指南', 'https://notion.so/wangwu-learning/webpack', 89),
(3, 'React Hooks详解', 'https://notion.so/wangwu-learning/react-hooks', 156),

-- 赵六的笔记
(4, '算法与数据结构', 'https://notion.so/zhaoliu-notes/algorithms', 145),
(4, 'LeetCode刷题记录', 'https://notion.org/zhaoliu-notes/leetcode', 112),
(4, '动态规划专题', 'https://notion.so/zhaoliu-notes/dynamic-programming', 87),
(4, '图论算法', 'https://notion.so/zhaoliu-notes/graph-algorithms', 65),

-- 孙七的笔记
(5, 'Linux系统管理', 'https://notion.so/sunqi-knowledge/linux-admin', 134),
(5, 'Docker容器化', 'https://notion.so/sunqi-knowledge/docker', 167),
(5, 'Kubernetes入门', 'https://notion.so/sunqi-knowledge/kubernetes', 123),
(5, 'CI/CD流水线', 'https://notion.so/sunqi-knowledge/cicd', 89),
(5, '微服务架构', 'https://notion.so/sunqi-knowledge/microservices', 156);

-- 插入敏感词数据
INSERT INTO `t_sensitive_word` (`word`, `type`, `status`) VALUES
('暴力', 3, 1),
('色情', 2, 1),
('赌博', 4, 1),
('毒品', 4, 1),
('诈骗', 4, 1),
('枪支', 3, 1),
('炸弹', 3, 1),
('政治敏感', 1, 1);

-- 更新用户的最后活跃时间（模拟不同时间）
UPDATE `t_user` SET `last_active_at` = DATE_SUB(NOW(), INTERVAL 1 HOUR) WHERE `id` = 1;
UPDATE `t_user` SET `last_active_at` = DATE_SUB(NOW(), INTERVAL 2 HOUR) WHERE `id` = 2;
UPDATE `t_user` SET `last_active_at` = DATE_SUB(NOW(), INTERVAL 3 HOUR) WHERE `id` = 3;
UPDATE `t_user` SET `last_active_at` = DATE_SUB(NOW(), INTERVAL 4 HOUR) WHERE `id` = 4;
UPDATE `t_user` SET `last_active_at` = DATE_SUB(NOW(), INTERVAL 5 HOUR) WHERE `id` = 5;

-- 更新笔记的更新时间（模拟不同时间）
UPDATE `t_note_item` SET `updated_at` = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 48) HOUR);

-- ============================================
-- 初始化完成
-- ============================================
SELECT '数据库初始化完成！' AS message;
SELECT '测试用户账号（密码均为: 123456）:' AS info;
SELECT username, email FROM t_user;
SELECT CONCAT('共插入 ', COUNT(*), ' 个用户') AS user_count FROM t_user;
SELECT CONCAT('共插入 ', COUNT(*), ' 个总链接') AS link_count FROM t_main_link;
SELECT CONCAT('共插入 ', COUNT(*), ' 个笔记') AS note_count FROM t_note_item;
SELECT CONCAT('共插入 ', COUNT(*), ' 个敏感词') AS word_count FROM t_sensitive_word;
