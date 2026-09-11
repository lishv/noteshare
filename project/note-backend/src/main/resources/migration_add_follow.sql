-- ==========================================================
-- 增量迁移脚本：添加用户关注表 t_user_follow
-- ==========================================================

CREATE TABLE IF NOT EXISTS `t_user_follow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关注主键 ID',
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
