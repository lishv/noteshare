-- 添加评论表迁移脚本
USE `note_sharing`;

CREATE TABLE IF NOT EXISTS `t_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `note_id` BIGINT NOT NULL COMMENT '所属笔记ID',
  `user_id` BIGINT NOT NULL COMMENT '发表评论的用户ID',
  `parent_id` BIGINT DEFAULT NULL COMMENT '根评论ID (若为顶级评论则为NULL)',
  `reply_to_user_id` BIGINT DEFAULT NULL COMMENT '被回复的目标用户ID',
  `reply_to_comment_id` BIGINT DEFAULT NULL COMMENT '被回复的具体评论ID',
  `content` VARCHAR(1000) NOT NULL COMMENT '评论文本内容',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-正常，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_note_created` (`note_id`, `created_at`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记评论表';
