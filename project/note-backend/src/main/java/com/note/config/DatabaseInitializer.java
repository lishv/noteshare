package com.note.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            String createCommentTableSql = """
                CREATE TABLE IF NOT EXISTS `t_comment` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论 ID',
                  `note_id` BIGINT NOT NULL COMMENT '所属笔记 ID',
                  `user_id` BIGINT NOT NULL COMMENT '发表评论的用户 ID',
                  `parent_id` BIGINT DEFAULT NULL COMMENT '根评论 ID',
                  `reply_to_user_id` BIGINT DEFAULT NULL COMMENT '被回复的目标用户 ID',
                  `reply_to_comment_id` BIGINT DEFAULT NULL COMMENT '被回复的具体评论 ID',
                  `content` VARCHAR(1000) NOT NULL COMMENT '评论正文',
                  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-正常，1-已删除',
                  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                  PRIMARY KEY (`id`),
                  KEY `idx_note_created` (`note_id`, `created_at`),
                  KEY `idx_user_id` (`user_id`),
                  KEY `idx_parent_id` (`parent_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记评论表';
            """;
            jdbcTemplate.execute(createCommentTableSql);
            log.info("DatabaseInitializer: t_comment table verified/created successfully.");
        } catch (Exception e) {
            log.warn("DatabaseInitializer error (can be ignored if table exists): {}", e.getMessage());
        }

        // 为已存在的评论表补充软删除索引，便于管理端按状态检索。
        try {
            jdbcTemplate.execute("ALTER TABLE `t_comment` ADD INDEX `idx_deleted_created` (`is_deleted`, `created_at`)");
            log.info("DatabaseInitializer: added comment deleted/created index.");
        } catch (Exception ignored) {
            // 索引已存在
        }

        try {
            jdbcTemplate.execute("ALTER TABLE `t_note_item` ADD COLUMN `is_public` TINYINT NOT NULL DEFAULT 1 COMMENT '是否公开：1-公开，0-私密' AFTER `is_pinned`");
            jdbcTemplate.execute("ALTER TABLE `t_note_item` ADD INDEX `idx_is_public` (`is_public`)");
            log.info("DatabaseInitializer: added column 'is_public' to t_note_item.");
        } catch (Exception ignored) {
            // 列或索引已存在
        }

        try {
            String createFollowTableSql = """
                CREATE TABLE IF NOT EXISTS `t_user_follow` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关注主键 ID',
                  `follower_id` BIGINT NOT NULL COMMENT '关注者用户 ID (粉丝)',
                  `following_id` BIGINT NOT NULL COMMENT '被关注的目标用户 ID (作者)',
                  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_follower_following` (`follower_id`, `following_id`),
                  KEY `idx_follower_id` (`follower_id`),
                  KEY `idx_following_id` (`following_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注表';
            """;
            jdbcTemplate.execute(createFollowTableSql);
            log.info("DatabaseInitializer: t_user_follow table verified/created successfully.");
        } catch (Exception e) {
            log.warn("DatabaseInitializer error creating t_user_follow: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE `t_user` ADD COLUMN `bio` VARCHAR(100) DEFAULT NULL COMMENT '个性签名 (最多30字)' AFTER `avatar`");
            log.info("DatabaseInitializer: added column 'bio' to t_user.");
        } catch (Exception ignored) {
            // 列已存在
        }

        try {
            jdbcTemplate.execute("ALTER TABLE `t_note_item` ADD COLUMN `like_count` BIGINT NOT NULL DEFAULT 0 COMMENT '点赞数' AFTER `view_count`");
            log.info("DatabaseInitializer: added column 'like_count' to t_note_item.");
        } catch (Exception ignored) {
            // 列已存在
        }

        try {
            String createLikeTableSql = """
                CREATE TABLE IF NOT EXISTS `t_note_like` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '点赞主键 ID',
                  `user_id` BIGINT NOT NULL COMMENT '点赞用户 ID',
                  `note_id` BIGINT NOT NULL COMMENT '笔记 ID',
                  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_user_note_like` (`user_id`, `note_id`),
                  KEY `idx_note_id` (`note_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记点赞表';
            """;
            jdbcTemplate.execute(createLikeTableSql);
            log.info("DatabaseInitializer: t_note_like table verified/created successfully.");
        } catch (Exception e) {
            log.warn("DatabaseInitializer error creating t_note_like: {}", e.getMessage());
        }

        try {
            String createNotificationTableSql = """
                CREATE TABLE IF NOT EXISTS `t_notification` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知主键 ID',
                  `user_id` BIGINT NOT NULL COMMENT '接收通知的用户 ID',
                  `sender_id` BIGINT DEFAULT NULL COMMENT '触发通知的用户 ID (系统通知为NULL)',
                  `type` VARCHAR(32) NOT NULL COMMENT '通知类型: COMMENT, FOLLOW, LIKE, SYSTEM',
                  `title` VARCHAR(128) NOT NULL COMMENT '通知简题',
                  `content` TEXT NOT NULL COMMENT '通知详细内容',
                  `target_type` VARCHAR(32) DEFAULT NULL COMMENT '关联对象类型: NOTE, USER, COMMENT, SYSTEM',
                  `target_id` BIGINT DEFAULT NULL COMMENT '关联对象ID',
                  `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
                  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否移除: 0-正常, 1-已移除',
                  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                  PRIMARY KEY (`id`),
                  KEY `idx_user_read` (`user_id`, `is_read`),
                  KEY `idx_user_deleted_read` (`user_id`, `is_deleted`, `is_read`),
                  KEY `idx_created` (`created_at`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内消息通知表';
            """;
            jdbcTemplate.execute(createNotificationTableSql);
            log.info("DatabaseInitializer: t_notification table verified/created successfully.");
        } catch (Exception e) {
            log.warn("DatabaseInitializer error creating t_notification: {}", e.getMessage());
        }

        // 为已存在的通知表补充用户移除消息所需的软删除字段。
        try {
            jdbcTemplate.execute("ALTER TABLE `t_notification` ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否移除: 0-正常, 1-已移除' AFTER `is_read`");
            log.info("DatabaseInitializer: added column 'is_deleted' to t_notification.");
        } catch (Exception ignored) {
            // 列已存在
        }

        try {
            jdbcTemplate.execute("ALTER TABLE `t_notification` ADD INDEX `idx_user_deleted_read` (`user_id`, `is_deleted`, `is_read`)");
            log.info("DatabaseInitializer: added notification user/deleted/read index.");
        } catch (Exception ignored) {
            // 索引已存在
        }
    }
}
