-- ============================================
-- 测试数据生成脚本
-- 创建日期：2026-05-13
-- 说明：生成完整的 RBAC 测试数据，包含用户、角色、菜单及关联关系
-- 密码：123456（BCrypt: $2a$10$SG7HkhWRQvmDzCG3S.aYsOxxezisRSSm.jRm4uXszZMdpf0g3JuMO）
-- ============================================

-- ============================================
-- 1. 用户数据
-- ============================================
INSERT INTO `sys_user` (`username`, `password`, `email`, `phone`, `status`) VALUES
('admin', '$2a$10$SG7HkhWRQvmDzCG3S.aYsOxxezisRSSm.jRm4uXszZMdpf0g3JuMO', 'admin@example.com', '13800138000', 1),
('manager', '$2a$10$SG7HkhWRQvmDzCG3S.aYsOxxezisRSSm.jRm4uXszZMdpf0g3JuMO', 'manager@example.com', '13800138001', 1),
('user1', '$2a$10$SG7HkhWRQvmDzCG3S.aYsOxxezisRSSm.jRm4uXszZMdpf0g3JuMO', 'user1@example.com', '13800138002', 1),
('user2', '$2a$10$SG7HkhWRQvmDzCG3S.aYsOxxezisRSSm.jRm4uXszZMdpf0g3JuMO', 'user2@example.com', '13800138003', 1),
('user3', '$2a$10$SG7HkhWRQvmDzCG3S.aYsOxxezisRSSm.jRm4uXszZMdpf0g3JuMO', 'user3@example.com', '13800138004', 1),
('viewer', '$2a$10$SG7HkhWRQvmDzCG3S.aYsOxxezisRSSm.jRm4uXszZMdpf0g3JuMO', 'viewer@example.com', '13800138005', 1),
('disabled_user', '$2a$10$SG7HkhWRQvmDzCG3S.aYsOxxezisRSSm.jRm4uXszZMdpf0g3JuMO', 'disabled@example.com', '13800138006', 0);

-- ============================================
-- 2. 角色数据
-- ============================================
INSERT INTO `sys_role` (`role_code`, `role_name`, `permission`, `level`, `data_scope`, `remark`) VALUES
('SUPER_ADMIN', '超级管理员', 'admin', 1, 'ALL', '系统超级管理员，拥有所有权限'),
('DEPT_ADMIN', '部门管理员', 'dept_admin', 10, 'DEPT_AND_SUB', '部门管理员，管理本部门及子部门数据'),
('MANAGER', '经理', 'manager', 20, 'DEPT', '部门经理，管理本部门数据'),
('EMPLOYEE', '员工', 'employee', 50, 'SELF', '普通员工，只能查看和操作自己的数据'),
('VIEWER', '只读用户', 'viewer', 99, 'SELF', '只读用户，仅查看权限');

-- ============================================
-- 3. 菜单数据（完整的菜单树）
-- ============================================
-- 一级菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(0, '系统管理', 'system', 1, '/system', NULL, NULL, 'system', 1, 1, 1, '系统管理模块'),
(0, '业务管理', 'business', 1, '/business', NULL, NULL, 'business', 2, 1, 1, '业务管理模块'),
(0, '报表统计', 'report', 1, '/report', NULL, NULL, 'chart', 3, 1, 1, '报表统计模块');

-- 系统管理 - 二级菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(1, '用户管理', 'system-user', 2, '/system/user', 'system/user/index', NULL, 'user', 1, 1, 1, '用户管理页面'),
(1, '角色管理', 'system-role', 2, '/system/role', 'system/role/index', NULL, 'role', 2, 1, 1, '角色管理页面'),
(1, '菜单管理', 'system-menu', 2, '/system/menu', 'system/menu/index', NULL, 'menu', 3, 1, 1, '菜单管理页面');

-- 用户管理 - 按钮权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(4, '用户查询', 'system-user-list', 3, NULL, NULL, 'user:list', NULL, 1, 0, 1, '查询用户列表'),
(4, '用户新增', 'system-user-add', 3, NULL, NULL, 'user:add', NULL, 2, 0, 1, '创建新用户'),
(4, '用户编辑', 'system-user-edit', 3, NULL, NULL, 'user:edit', NULL, 3, 0, 1, '编辑用户信息'),
(4, '用户删除', 'system-user-delete', 3, NULL, NULL, 'user:delete', NULL, 4, 0, 1, '删除用户');

-- 角色管理 - 按钮权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(5, '角色查询', 'system-role-list', 3, NULL, NULL, 'role:list', NULL, 1, 0, 1, '查询角色列表'),
(5, '角色新增', 'system-role-add', 3, NULL, NULL, 'role:add', NULL, 2, 0, 1, '创建新角色'),
(5, '角色编辑', 'system-role-edit', 3, NULL, NULL, 'role:edit', NULL, 3, 0, 1, '编辑角色信息'),
(5, '角色删除', 'system-role-delete', 3, NULL, NULL, 'role:delete', NULL, 4, 0, 1, '删除角色');

-- 菜单管理 - 按钮权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(6, '菜单查询', 'system-menu-list', 3, NULL, NULL, 'menu:list', NULL, 1, 0, 1, '查询菜单列表'),
(6, '菜单新增', 'system-menu-add', 3, NULL, NULL, 'menu:add', NULL, 2, 0, 1, '创建新菜单'),
(6, '菜单编辑', 'system-menu-edit', 3, NULL, NULL, 'menu:edit', NULL, 3, 0, 1, '编辑菜单信息'),
(6, '菜单删除', 'system-menu-delete', 3, NULL, NULL, 'menu:delete', NULL, 4, 0, 1, '删除菜单');

-- 业务管理 - 二级菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(2, '订单管理', 'business-order', 2, '/business/order', 'business/order/index', NULL, 'order', 1, 1, 1, '订单管理页面'),
(2, '客户管理', 'business-customer', 2, '/business/customer', 'business/customer/index', NULL, 'customer', 2, 1, 1, '客户管理页面');

-- 订单管理 - 按钮权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(19, '订单查询', 'business-order-list', 3, NULL, NULL, 'order:list', NULL, 1, 0, 1, '查询订单列表'),
(19, '订单新增', 'business-order-add', 3, NULL, NULL, 'order:add', NULL, 2, 0, 1, '创建新订单'),
(19, '订单编辑', 'business-order-edit', 3, NULL, NULL, 'order:edit', NULL, 3, 0, 1, '编辑订单信息'),
(19, '订单删除', 'business-order-delete', 3, NULL, NULL, 'order:delete', NULL, 4, 0, 1, '删除订单');

-- 客户管理 - 按钮权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(20, '客户查询', 'business-customer-list', 3, NULL, NULL, 'customer:list', NULL, 1, 0, 1, '查询客户列表'),
(20, '客户新增', 'business-customer-add', 3, NULL, NULL, 'customer:add', NULL, 2, 0, 1, '创建新客户'),
(20, '客户编辑', 'business-customer-edit', 3, NULL, NULL, 'customer:edit', NULL, 3, 0, 1, '编辑客户信息'),
(20, '客户删除', 'business-customer-delete', 3, NULL, NULL, 'customer:delete', NULL, 4, 0, 1, '删除客户');

-- 报表统计 - 二级菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(3, '销售报表', 'report-sales', 2, '/report/sales', 'report/sales/index', NULL, 'chart-line', 1, 1, 1, '销售数据统计报表'),
(3, '用户报表', 'report-user', 2, '/report/user', 'report/user/index', NULL, 'chart-bar', 2, 1, 1, '用户数据统计报表');

-- 报表 - 按钮权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(32, '查看报表', 'report-sales-view', 3, NULL, NULL, 'report:view', NULL, 1, 0, 1, '查看销售报表'),
(33, '查看报表', 'report-user-view', 3, NULL, NULL, 'report:view', NULL, 1, 0, 1, '查看用户报表');

-- ============================================
-- 4. 用户-角色关联数据
-- ============================================
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
-- admin 拥有超级管理员角色
(1, 1),
-- manager 拥有部门管理员和经理角色
(2, 2),
(2, 3),
-- user1, user2, user3 拥有员工角色
(3, 4),
(4, 4),
(5, 4),
-- viewer 拥有只读角色
(6, 5);

-- ============================================
-- 5. 角色-菜单关联数据
-- ============================================
-- 超级管理员：拥有所有菜单权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, id FROM `sys_menu`;

-- 部门管理员：拥有系统管理模块 + 业务管理模块的完整权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, id FROM `sys_menu` WHERE id IN (
    1, 4, 7, 8, 9, 10, 5, 11, 12, 13, 14, 6, 15, 16, 17, 18,  -- 系统管理
    2, 19, 21, 22, 23, 24, 20, 25, 26, 27, 28                  -- 业务管理
);

-- 经理：拥有业务管理模块 + 报表查看权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 3, id FROM `sys_menu` WHERE id IN (
    2, 19, 21, 22, 23, 24, 20, 25, 26, 27, 28,  -- 业务管理
    3, 32, 34, 33, 35                            -- 报表统计
);

-- 员工：拥有订单查询、客户查询权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 4, id FROM `sys_menu` WHERE id IN (
    2, 19, 21, 20, 25
);

-- 只读用户：只有查看权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 5, id FROM `sys_menu` WHERE id IN (
    2, 19, 21, 20, 25, 3, 32, 34, 33, 35
);

-- ============================================
-- 6. 登录审计日志示例数据
-- ============================================
INSERT INTO `sys_login_log` (`username`, `login_ip`, `status`, `fail_reason`, `login_time`) VALUES
('admin', '192.168.1.100', 1, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
('manager', '192.168.1.101', 1, NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
('user1', '192.168.1.102', 1, NULL, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
('user2', '192.168.1.103', 0, '密码错误', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
('user2', '192.168.1.103', 1, NULL, DATE_SUB(NOW(), INTERVAL 29 MINUTE)),
('viewer', '192.168.1.105', 1, NULL, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
('disabled_user', '192.168.1.106', 0, '账户已禁用', DATE_SUB(NOW(), INTERVAL 5 MINUTE));

-- ============================================
-- 测试数据说明
-- ============================================
-- 用户账号：
--   admin / 123456       - 超级管理员，拥有所有权限
--   manager / 123456     - 部门管理员 + 经理，拥有系统管理和业务管理权限
--   user1 / 123456       - 普通员工，拥有基本业务查询权限
--   user2 / 123456       - 普通员工，拥有基本业务查询权限
--   user3 / 123456       - 普通员工，拥有基本业务查询权限
--   viewer / 123456      - 只读用户，仅查看权限
--   disabled_user / 123456 - 已禁用账户
--
-- 菜单结构：
--   系统管理（用户、角色、菜单）
--   业务管理（订单、客户）
--   报表统计（销售报表、用户报表）
--
-- 数据权限范围：
--   ALL - 全部数据权限
--   DEPT_AND_SUB - 部门及子部门数据权限
--   DEPT - 部门数据权限
--   SELF - 仅本人数据权限
