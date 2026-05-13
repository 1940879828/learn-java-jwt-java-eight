-- ============================================
-- JWT + RBAC 完整数据库初始化脚本
-- MySQL 5.7.44
-- 创建日期：2026-05-13
-- 说明：包含认证系统 + RBAC权限系统的完整表结构和初始数据
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS jwt_java_eight DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE jwt_java_eight;

-- ============================================
-- 1. 用户表（包含登录安全字段和联系方式）
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`              BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`        VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`        VARCHAR(128) NOT NULL COMMENT '密码(BCrypt)',
    `email`           VARCHAR(255) DEFAULT NULL COMMENT '邮箱',
    `phone`           VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `status`          TINYINT(4)   NOT NULL DEFAULT 1 COMMENT '状态: 1-正常 0-禁用',
    `failed_attempts` INT(11)      NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    `lock_time`       DATETIME     DEFAULT NULL COMMENT '锁定时间（NULL表示未锁定），锁定时长2小时后自动解锁',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ============================================
-- 2. 登录审计日志表
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_login_log` (
    `id`          BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(64)  NOT NULL COMMENT '登录用户名',
    `login_ip`    VARCHAR(45)  DEFAULT NULL COMMENT '登录IP',
    `status`      TINYINT(4)   NOT NULL COMMENT '登录结果: 1-成功 0-失败',
    `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    `login_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (`id`),
    KEY `idx_username` (`username`),
    KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录审计日志表';

-- ============================================
-- 3. 刷新令牌表
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_refresh_token` (
    `id`          BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT(20)   NOT NULL COMMENT '用户ID',
    `token_hash`  VARCHAR(64)  NOT NULL COMMENT 'Refresh Token的SHA256哈希',
    `expire_time` DATETIME     NOT NULL COMMENT '过期时间',
    `device_info` VARCHAR(255) DEFAULT NULL COMMENT '设备信息',
    `jti_id`      VARCHAR(64)  NOT NULL COMMENT 'JWT唯一标识',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_jti` (`jti_id`),
    KEY `idx_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='刷新令牌表';

-- ============================================
-- 4. 角色表
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id`          INT(11)      NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_code`   VARCHAR(50)  NOT NULL COMMENT '角色编码',
    `role_name`   VARCHAR(50)  NOT NULL COMMENT '角色名称',
    `permission`  VARCHAR(255) DEFAULT NULL COMMENT '角色权限字符串',
    `level`       INT(11)      DEFAULT 0 COMMENT '角色级别',
    `data_scope`  VARCHAR(50)  DEFAULT NULL COMMENT '数据权限范围',
    `create_by`   VARCHAR(50)  DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- ============================================
-- 5. 菜单/权限表
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_menu` (
    `id`          INT(11)      NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `parent_id`   INT(11)      DEFAULT 0 COMMENT '父菜单ID',
    `menu_name`   VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    `menu_code`   VARCHAR(100) NOT NULL COMMENT '菜单编码/权限标识',
    `menu_type`   TINYINT(4)   NOT NULL DEFAULT 1 COMMENT '菜单类型：1-菜单，2-按钮，3-接口',
    `path`        VARCHAR(200) DEFAULT NULL COMMENT '路由路径',
    `component`   VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
    `perms`       VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    `icon`        VARCHAR(100) DEFAULT NULL COMMENT '菜单图标',
    `sort_order`  INT(11)      DEFAULT 0 COMMENT '排序',
    `visible`     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否可见：0-隐藏，1-显示',
    `status`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `create_by`   VARCHAR(50)  DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_menu_code` (`menu_code`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单权限表';

-- ============================================
-- 6. 用户-角色关联表
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id`          INT(11)    NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT(20) NOT NULL COMMENT '用户ID',
    `role_id`     INT(11)    NOT NULL COMMENT '角色ID',
    `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';

-- ============================================
-- 7. 角色-菜单关联表
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
    `id`          INT(11)  NOT NULL AUTO_INCREMENT,
    `role_id`     INT(11)  NOT NULL COMMENT '角色ID',
    `menu_id`     INT(11)  NOT NULL COMMENT '菜单ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单关联表';

-- ============================================
-- 8. 初始化系统管理员用户
-- ============================================
-- 用户名: admin
-- 密码: 123456
-- BCrypt加密: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EHCwf0r.IQmBz0Uoz4Lx1K
INSERT INTO `sys_user` (`username`, `password`, `email`, `phone`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EHCwf0r.IQmBz0Uoz4Lx1K', 'admin@system.local', NULL, 1);

-- ============================================
-- 9. 初始化默认角色
-- ============================================
INSERT INTO `sys_role` (`role_code`, `role_name`, `permission`, `level`, `data_scope`, `remark`) VALUES
('SUPER_ADMIN', '超级管理员', 'admin', 1, 'ALL', '系统超级管理员，拥有所有权限'),
('NORMAL_USER', '普通用户', 'user', 99, 'SELF', '普通用户角色');

-- ============================================
-- 10. 初始化基础菜单权限
-- ============================================
-- 一级菜单：系统管理
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(0, '系统管理', 'system', 1, '/system', NULL, 'system', 1, 1, 1, '系统管理模块');

-- 二级菜单：用户管理
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(1, '用户管理', 'system-user', 1, '/system/user', NULL, 'user', 1, 1, 1, '用户管理页面');

-- 用户管理按钮权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(2, '用户查询', 'system-user-list', 2, NULL, 'user:list', NULL, 1, 0, 1, '查询用户列表'),
(2, '用户新增', 'system-user-add', 2, NULL, 'user:add', NULL, 2, 0, 1, '创建新用户'),
(2, '用户编辑', 'system-user-edit', 2, NULL, 'user:edit', NULL, 3, 0, 1, '编辑用户信息'),
(2, '用户删除', 'system-user-delete', 2, NULL, 'user:delete', NULL, 4, 0, 1, '删除用户');

-- 二级菜单：角色管理
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(1, '角色管理', 'system-role', 1, '/system/role', NULL, 'role', 2, 1, 1, '角色管理页面');

-- 角色管理按钮权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(7, '角色查询', 'system-role-list', 2, NULL, 'role:list', NULL, 1, 0, 1, '查询角色列表'),
(7, '角色新增', 'system-role-add', 2, NULL, 'role:add', NULL, 2, 0, 1, '创建新角色'),
(7, '角色编辑', 'system-role-edit', 2, NULL, 'role:edit', NULL, 3, 0, 1, '编辑角色信息'),
(7, '角色删除', 'system-role-delete', 2, NULL, 'role:delete', NULL, 4, 0, 1, '删除角色'),
(7, '角色强制删除', 'system-role-force-delete', 2, NULL, 'role:force-delete', NULL, 5, 0, 1, '强制删除角色（即使被引用）');

-- 二级菜单：菜单管理
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(1, '菜单管理', 'system-menu', 1, '/system/menu', NULL, 'menu', 3, 1, 1, '菜单管理页面');

-- 菜单管理按钮权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(13, '菜单查询', 'system-menu-list', 2, NULL, 'menu:list', NULL, 1, 0, 1, '查询菜单列表'),
(13, '菜单新增', 'system-menu-add', 2, NULL, 'menu:add', NULL, 2, 0, 1, '创建新菜单'),
(13, '菜单编辑', 'system-menu-edit', 2, NULL, 'menu:edit', NULL, 3, 0, 1, '编辑菜单信息'),
(13, '菜单删除', 'system-menu-delete', 2, NULL, 'menu:delete', NULL, 4, 0, 1, '删除菜单');

-- 开发工具菜单（仅开发环境）
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(1, '开发工具', 'system-dev', 1, '/system/dev', NULL, 'tool', 99, 1, 1, '开发工具页面（仅开发环境）'),
(18, 'API文档导出', 'system-dev-apidoc', 2, NULL, 'system:dev-tools', NULL, 1, 0, 1, '导出OpenAPI文档（仅开发环境）');

-- ============================================
-- 11. 给超级管理员分配所有权限
-- ============================================
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, id FROM `sys_menu`;

-- ============================================
-- 12. 给普通用户分配基础查询权限
-- ============================================
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(2, 1),  -- 系统管理模块
(2, 2),  -- 用户管理页面
(2, 3),  -- 用户查询
(2, 7),  -- 角色管理页面
(2, 8);  -- 角色查询

-- ============================================
-- 13. 给admin用户分配超级管理员角色
-- ============================================
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- ============================================
-- 初始化完成说明
-- ============================================
-- 默认管理员账号：
--   用户名: admin
--   密码:   123456
--   角色:   超级管理员（拥有所有权限）
--
-- 数据权限范围说明：
--   ALL          - 全部数据权限
--   DEPT_AND_SUB - 部门及子部门数据权限
--   DEPT         - 部门数据权限
--   SELF         - 仅本人数据权限
--
-- 菜单类型说明：
--   1 - 目录/菜单（有路由）
--   2 - 按钮/权限（无路由，用于按钮级权限控制）
--   3 - 接口（预留，用于接口级权限控制）
