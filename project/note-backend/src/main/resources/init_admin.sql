-- ========================================
-- 学生笔记共享平台 - 管理端数据库初始化脚本
-- 包含管理员表、管理员登录失败记录表、操作日志表及默认超级管理员数据
-- ========================================
USE `note_sharing`;
-- 删除已存在的表（按依赖顺序）
DROP TABLE IF EXISTS `t_operation_log`;
DROP TABLE IF EXISTS `t_admin_login_fail_record`;
DROP TABLE IF EXISTS `t_admin`;
-- ========================================
-- 1. 管理员表
-- ========================================
CREATE TABLE `t_admin` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username` VARCHAR(50) NOT NULL COMMENT '管理员用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `role` TINYINT NOT NULL DEFAULT 2 COMMENT '角色: 1-超级管理员, 2-普通管理员',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-正常, 0-禁用',
  `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role_status` (`role`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '管理员表';
-- ========================================
-- 2. 管理员登录失败记录表
-- ========================================
CREATE TABLE `t_admin_login_fail_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '管理员用户名',
  `ip_address` VARCHAR(50) NOT NULL COMMENT 'IP地址',
  `fail_count` INT NOT NULL DEFAULT 1 COMMENT '失败次数',
  `lock_until` DATETIME DEFAULT NULL COMMENT '锁定到期时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username_ip` (`username`, `ip_address`),
  KEY `idx_lock_until` (`lock_until`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '管理员登录失败记录表';
-- ========================================
-- 3. 操作日志表（仅插入不更新，无 updated_at 字段）
-- ========================================
CREATE TABLE `t_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `admin_id` BIGINT NOT NULL COMMENT '操作管理员ID',
  `admin_username` VARCHAR(50) NOT NULL COMMENT '操作管理员用户名',
  `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型: LOGIN/USER_MANAGE/NOTE_MANAGE/LINK_MANAGE/WORD_MANAGE/ADMIN_MANAGE',
  `description` VARCHAR(500) NOT NULL COMMENT '操作描述',
  `detail` TEXT DEFAULT NULL COMMENT '操作详情/附加信息',
  `target_type` VARCHAR(50) DEFAULT NULL COMMENT '操作对象类型: USER/NOTE/MAIN_LINK/SENSITIVE_WORD/ADMIN',
  `target_id` BIGINT DEFAULT NULL COMMENT '操作对象ID',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_type_created_at` (`operation_type`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '操作日志表';
-- ========================================
-- 4. 默认超级管理员数据
-- 默认账号: admin
-- 默认密码: admin123
-- BCrypt hash for 'admin123': $2a$10$akZrr8BZ43N3XtPv98s/AODuSeitef1KOlSfYED3Clot7rlzkdKAy
-- 首次登录后请立即修改密码
-- ========================================
INSERT INTO `t_admin` (
    `username`,
    `password`,
    `email`,
    `role`,
    `status`
  )
VALUES (
    'admin',
    '$2a$10$akZrr8BZ43N3XtPv98s/AODuSeitef1KOlSfYED3Clot7rlzkdKAy',
    'admin@example.com',
    1,
    1
  );
-- ========================================
-- 数据统计信息
-- ========================================
SELECT '管理端数据库初始化完成！' AS message;
SELECT '管理员数量' AS type,
  COUNT(*) AS count
FROM `t_admin`;
SELECT '操作日志表' AS type,
  '已创建' AS status
FROM dual;
SELECT '管理员登录失败记录表' AS type,
  '已创建' AS status
FROM dual;