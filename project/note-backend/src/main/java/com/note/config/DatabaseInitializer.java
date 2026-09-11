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
    }
}
