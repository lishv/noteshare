-- 学生笔记共享平台数据库初始化脚本（包含测试数据）

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `note_sharing` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `note_sharing`;

-- 删除已存在的表（按依赖顺序）
DROP TABLE IF EXISTS `t_comment`;
DROP TABLE IF EXISTS `t_note_tag`;
DROP TABLE IF EXISTS `t_favorite`;
DROP TABLE IF EXISTS `t_note_item`;
DROP TABLE IF EXISTS `t_main_link`;
DROP TABLE IF EXISTS `t_login_fail_record`;
DROP TABLE IF EXISTS `t_sensitive_word`;
DROP TABLE IF EXISTS `t_user`;

-- 用户表
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
CREATE TABLE `t_note_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '笔记 ID',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `title` VARCHAR(100) NOT NULL COMMENT '笔记名称',
  `note_type` TINYINT NOT NULL DEFAULT 1 COMMENT '笔记类型：1-链接笔记, 2-创作笔记(Markdown)',
  `location_or_url` VARCHAR(500) DEFAULT NULL COMMENT '位置或链接',
  `content` MEDIUMTEXT DEFAULT NULL COMMENT '笔记正文内容 (Markdown格式)',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '笔记摘要',
  `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图URL',
  `view_count` BIGINT NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：1-已删除，0-未删除',
  `is_pinned` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：1-已置顶，0-未置顶',
  `is_public` TINYINT NOT NULL DEFAULT 1 COMMENT '是否公开：1-公开，0-私密',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id_updated_at` (`user_id`, `updated_at`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_note_type` (`note_type`),
  KEY `idx_is_public` (`is_public`),
  KEY `idx_updated_at` (`updated_at`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `fk_note_item_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记表';

-- 笔记评论表
CREATE TABLE `t_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论 ID',
  `note_id` BIGINT NOT NULL COMMENT '所属笔记 ID',
  `user_id` BIGINT NOT NULL COMMENT '发表评论的用户 ID',
  `parent_id` BIGINT DEFAULT NULL COMMENT '根评论 ID (顶级评论为 NULL)',
  `reply_to_user_id` BIGINT DEFAULT NULL COMMENT '被回复的目标用户 ID',
  `reply_to_comment_id` BIGINT DEFAULT NULL COMMENT '被回复的具体评论 ID',
  `content` VARCHAR(1000) NOT NULL COMMENT '评论正文',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-正常，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_note_created` (`note_id`, `created_at`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  CONSTRAINT `fk_comment_note` FOREIGN KEY (`note_id`) REFERENCES `t_note_item` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记评论表';

-- 登录失败记录表
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录失败记录表';

-- 敏感词表
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

-- 用户关注表
CREATE TABLE `t_user_follow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `follower_id` BIGINT NOT NULL COMMENT '关注者用户 ID (粉丝)',
  `following_id` BIGINT NOT NULL COMMENT '被关注的目标用户 ID (作者)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follower_following` (`follower_id`, `following_id`),
  KEY `idx_follower_id` (`follower_id`),
  KEY `idx_following_id` (`following_id`),
  CONSTRAINT `fk_follow_follower` FOREIGN KEY (`follower_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_follow_following` FOREIGN KEY (`following_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注表';

-- ====================================
-- 插入测试数据
-- ====================================

-- 插入测试用户（密码均为: password123）
-- BCrypt 加密后的 password123: $2a$10$KxHI20qJ2K4OASCBKrkIOe3irGtHEd5lTg4xftOU0sZHG3LYZuzx6
INSERT INTO `t_user` (`username`, `password`, `email`, `phone`, `avatar`, `status`, `last_active_at`, `created_at`, `updated_at`) VALUES
('zhangsan', '$2a$10$KxHI20qJ2K4OASCBKrkIOe3irGtHEd5lTg4xftOU0sZHG3LYZuzx6', 'zhangsan@example.com', '13800138001', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhangsan', 1, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 2 DAY),
('lisi', '$2a$10$KxHI20qJ2K4OASCBKrkIOe3irGtHEd5lTg4xftOU0sZHG3LYZuzx6', 'lisi@example.com', '13800138002', 'https://api.dicebear.com/7.x/avataaars/svg?seed=lisi', 1, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 1 DAY),
('wangwu', '$2a$10$KxHI20qJ2K4OASCBKrkIOe3irGtHEd5lTg4xftOU0sZHG3LYZuzx6', 'wangwu@example.com', '13800138003', 'https://api.dicebear.com/7.x/avataaars/svg?seed=wangwu', 1, NOW(), NOW() - INTERVAL 20 DAY, NOW()),
('zhaoliu', '$2a$10$KxHI20qJ2K4OASCBKrkIOe3irGtHEd5lTg4xftOU0sZHG3LYZuzx6', 'zhaoliu@example.com', '13800138004', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhaoliu', 1, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 3 DAY),
('qianqi', '$2a$10$KxHI20qJ2K4OASCBKrkIOe3irGtHEd5lTg4xftOU0sZHG3LYZuzx6', 'qianqi@example.com', '13800138005', 'https://api.dicebear.com/7.x/avataaars/svg?seed=qianqi', 1, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 5 DAY);

-- 插入总链接测试数据
INSERT INTO `t_main_link` (`user_id`, `main_url`, `view_count`, `created_at`, `updated_at`) VALUES
(1, 'https://notion.so/zhangsan', 156, NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 2 DAY),
(2, 'https://notion.so/lisi', 89, NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 1 DAY),
(3, 'https://notion.so/wangwu', 234, NOW() - INTERVAL 20 DAY, NOW()),
(4, 'https://notion.so/zhaoliu', 45, NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 3 DAY);

-- 插入笔记测试数据
INSERT INTO `t_note_item` (`user_id`, `title`, `location_or_url`, `view_count`, `is_deleted`, `created_at`, `updated_at`) VALUES
-- 张三的笔记
(1, 'Java基础学习笔记', 'https://notion.so/zhangsan/java-basics', 312, 0, NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 2 DAY),
(1, 'Spring Boot入门教程', 'https://notion.so/zhangsan/spring-boot', 256, 0, NOW() - INTERVAL 28 DAY, NOW() - INTERVAL 1 DAY),
(1, 'MySQL数据库优化', 'https://notion.so/zhangsan/mysql-optimization', 189, 0, NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 3 DAY),
(1, 'Redis缓存策略', 'https://notion.so/zhangsan/redis-cache', 145, 0, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 5 DAY),
(1, '算法与数据结构', 'https://notion.so/zhangsan/algorithms', 234, 0, NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 1 DAY),

-- 李四的笔记
(2, 'Python数据分析', 'https://notion.so/lisi/python-data', 178, 0, NOW() - INTERVAL 24 DAY, NOW() - INTERVAL 1 DAY),
(2, '机器学习入门', 'https://notion.so/lisi/ml-intro', 267, 0, NOW() - INTERVAL 23 DAY, NOW() - INTERVAL 2 DAY),
(2, 'Docker容器化部署', 'https://notion.so/lisi/docker-deploy', 145, 0, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 3 DAY),
(2, 'Linux常用命令', 'https://notion.so/lisi/linux-commands', 198, 0, NOW() - INTERVAL 18 DAY, NOW() - INTERVAL 1 DAY),

-- 王五的笔记
(3, '前端开发基础', 'https://notion.so/wangwu/frontend-basics', 345, 0, NOW() - INTERVAL 19 DAY, NOW()),
(3, 'Vue3实战项目', 'https://notion.so/wangwu/vue3-project', 289, 0, NOW() - INTERVAL 18 DAY, NOW() - INTERVAL 1 DAY),
(3, 'React Hooks详解', 'https://notion.so/wangwu/react-hooks', 234, 0, NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 2 DAY),
(3, 'TypeScript进阶', 'https://notion.so/wangwu/typescript-advanced', 178, 0, NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 1 DAY),
(3, 'Webpack配置指南', 'https://notion.so/wangwu/webpack-config', 156, 0, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 3 DAY),

-- 赵六的笔记
(4, 'Go语言并发编程', 'https://notion.so/zhaoliu/go-concurrency', 123, 0, NOW() - INTERVAL 14 DAY, NOW() - INTERVAL 3 DAY),
(4, '微服务架构设计', 'https://notion.so/zhaoliu/microservices', 267, 0, NOW() - INTERVAL 13 DAY, NOW() - INTERVAL 2 DAY),
(4, 'Kubernetes集群管理', 'https://notion.so/zhaoliu/kubernetes', 234, 0, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 1 DAY),

-- 钱七的笔记
(5, '网络安全基础', 'https://notion.so/qianqi/security-basics', 189, 0, NOW() - INTERVAL 9 DAY, NOW() - INTERVAL 5 DAY),
(5, '渗透测试实战', 'https://notion.so/qianqi/pentesting', 234, 0, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 2 DAY),
(5, '密码学原理', 'https://notion.so/qianqi/cryptography', 156, 0, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 1 DAY);

-- 插入敏感词测试数据
INSERT INTO `t_sensitive_word` (`word`, `type`, `status`, `created_at`) VALUES
('暴力', 3, 1, NOW()),
('色情', 2, 1, NOW()),
('赌博', 4, 1, NOW()),
('毒品', 4, 1, NOW()),
('枪支', 3, 1, NOW()),
('恐怖主义', 1, 1, NOW()),
('诈骗', 4, 1, NOW()),
('黑客', 4, 1, NOW());

-- ====================================
-- 数据统计信息
-- ====================================
SELECT '数据库初始化完成！' AS message;
SELECT '用户数量' AS type, COUNT(*) AS count FROM `t_user`;
SELECT '总链接数量' AS type, COUNT(*) AS count FROM `t_main_link`;
SELECT '笔记数量' AS type, COUNT(*) AS count FROM `t_note_item` WHERE `is_deleted` = 0;
SELECT '敏感词数量' AS type, COUNT(*) AS count FROM `t_sensitive_word` WHERE `status` = 1;
