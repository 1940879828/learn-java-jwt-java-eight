-- ============================================
-- 添加用户表的邮箱和手机字段
-- 创建日期：2026-05-13
-- 说明：支持注册时填写邮箱，邮箱必须唯一
-- ============================================

ALTER TABLE `sys_user` ADD COLUMN `email` VARCHAR(255) DEFAULT NULL COMMENT '邮箱' AFTER `password`;
ALTER TABLE `sys_user` ADD COLUMN `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号' AFTER `email`;
ALTER TABLE `sys_user` ADD UNIQUE KEY `uk_email` (`email`);
