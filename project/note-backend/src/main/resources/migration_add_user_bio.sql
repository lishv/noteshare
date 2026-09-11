-- 添加用户个性签名字段
ALTER TABLE `t_user` ADD COLUMN `bio` VARCHAR(100) DEFAULT NULL COMMENT '个性签名 (最多30字)' AFTER `avatar`;
