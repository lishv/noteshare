-- =======================================================
-- 数据库升级迁移脚本：支持“创作笔记 (Markdown)”功能
-- 目标数据库：note_sharing
-- 适用表：t_note_item
-- =======================================================

USE `note_sharing`;

-- 1. 为 t_note_item 添加 note_type 字段（1-链接笔记，2-创作笔记）
ALTER TABLE `t_note_item`
ADD COLUMN `note_type` TINYINT NOT NULL DEFAULT 1 COMMENT '笔记类型：1-链接笔记, 2-创作笔记(Markdown)' AFTER `title`;

-- 2. 为 t_note_item 添加 content 字段（Markdown 正文内容）
ALTER TABLE `t_note_item`
ADD COLUMN `content` MEDIUMTEXT DEFAULT NULL COMMENT '笔记正文内容 (Markdown格式)' AFTER `location_or_url`;

-- 3. 为 t_note_item 添加 summary 字段（列表摘要）
ALTER TABLE `t_note_item`
ADD COLUMN `summary` VARCHAR(500) DEFAULT NULL COMMENT '笔记摘要' AFTER `content`;

-- 4. 为 t_note_item 添加 cover_image 字段（封面图）
ALTER TABLE `t_note_item`
ADD COLUMN `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图URL' AFTER `summary`;

-- 5. 添加 note_type 检索索引
ALTER TABLE `t_note_item`
ADD INDEX `idx_note_type` (`note_type`);

-- 6. （可选安全检查）如果旧表中没有 is_pinned 字段，请解除下方注释执行：
-- ALTER TABLE `t_note_item` ADD COLUMN `is_pinned` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：1-已置顶，0-未置顶' AFTER `is_deleted`;
