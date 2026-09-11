-- 增加笔记公开/私有可见性字段
USE `note_sharing`;

ALTER TABLE `t_note_item`
  ADD COLUMN `is_public` TINYINT NOT NULL DEFAULT 1 COMMENT '是否公开：1-公开，0-私密' AFTER `is_pinned`,
  ADD INDEX `idx_is_public` (`is_public`);
