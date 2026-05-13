-- ============================================
-- RBAC 权限系统表结构
-- 创建日期：2026-05-13
-- 说明：包含角色、菜单、用户-角色、角色-菜单关联表
-- ============================================

-- 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `permission` VARCHAR(255) DEFAULT NULL COMMENT '角色权限字符串',
  `level` INT(11) DEFAULT 0 COMMENT '角色级别',
  `data_scope` VARCHAR(50) DEFAULT NULL COMMENT '数据权限',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 菜单/权限表
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `parent_id` INT(11) DEFAULT 0 COMMENT '父菜单ID',
  `menu_name` VARCHAR(50) NOT NULL COMMENT '菜单名称',
  `menu_code` VARCHAR(100) NOT NULL COMMENT '菜单编码/权限标识',
  `menu_type` TINYINT(4) NOT NULL DEFAULT 1 COMMENT '菜单类型：1-菜单，2-按钮，3-接口',
  `path` VARCHAR(200) DEFAULT NULL COMMENT '路由路径',
  `component` VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
  `perms` VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '菜单图标',
  `sort_order` INT(11) DEFAULT 0 COMMENT '排序',
  `visible` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可见：0-隐藏，1-显示',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_menu_code` (`menu_code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单权限表';

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `role_id` INT(11) NOT NULL COMMENT '角色ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';

-- 角色-菜单关联表
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `role_id` INT(11) NOT NULL COMMENT '角色ID',
  `menu_id` INT(11) NOT NULL COMMENT '菜单ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单关联表';

-- ============================================
-- 初始化默认数据
-- ============================================

-- 插入默认角色
INSERT INTO `sys_role` (`role_code`, `role_name`, `permission`, `level`, `remark`) VALUES
('ROLE_ADMIN', '超级管理员', 'admin', 1, '系统超级管理员，拥有所有权限'),
('ROLE_USER', '普通用户', 'user', 99, '普通用户角色');

-- 插入默认菜单权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `perms`, `sort_order`, `remark`) VALUES
(0, '系统管理', 'system', 1, '/system', NULL, 1, '系统管理菜单'),
(1, '用户管理', 'system:user', 1, '/system/user', NULL, 1, '用户管理菜单'),
(2, '用户查询', 'system:user:list', 2, NULL, 'user:list', 1, '用户列表查询权限'),
(2, '用户新增', 'system:user:add', 2, NULL, 'user:add', 2, '用户新增权限'),
(2, '用户编辑', 'system:user:edit', 2, NULL, 'user:edit', 3, '用户编辑权限'),
(2, '用户删除', 'system:user:delete', 2, NULL, 'user:delete', 4, '用户删除权限'),
(1, '角色管理', 'system:role', 1, '/system/role', NULL, 2, '角色管理菜单'),
(7, '角色查询', 'system:role:list', 2, NULL, 'role:list', 1, '角色列表查询权限'),
(7, '角色新增', 'system:role:add', 2, NULL, 'role:add', 2, '角色新增权限'),
(7, '角色编辑', 'system:role:edit', 2, NULL, 'role:edit', 3, '角色编辑权限'),
(7, '角色删除', 'system:role:delete', 2, NULL, 'role:delete', 4, '角色删除权限');

-- 给超级管理员分配所有权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, id FROM `sys_menu`;

-- 给普通用户分配查询权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 3), (2, 8);
