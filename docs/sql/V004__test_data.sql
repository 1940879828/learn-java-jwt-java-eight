-- ============================================
-- 测试数据生成脚本（优化版）
-- 创建日期：2026-05-13
-- 前置条件：必须先执行 init_data.sql
-- 说明：生成完整的 RBAC 测试数据，扩展业务模块
-- 密码：123456（BCrypt: $2a$10$SG7HkhWRQvmDzCG3S.aYsOxxezisRSSm.jRm4uXszZMdpf0g3JuMO）
-- ============================================

USE jwt_java_eight;

-- ============================================
-- 1. 扩展角色数据（基于init_data.sql已有的2个角色）
-- ============================================
-- init_data.sql 已创建：
--   id=1: SUPER_ADMIN (超级管理员)
--   id=2: NORMAL_USER (普通用户)
-- 现在添加更多细分角色：
INSERT INTO `sys_role` (`role_code`, `role_name`, `permission`, `level`, `data_scope`, `remark`) VALUES
('DEPT_ADMIN', '部门管理员', 'dept_admin', 10, 'DEPT_AND_SUB', '部门管理员，管理本部门及子部门数据'),
('MANAGER', '经理', 'manager', 20, 'DEPT', '部门经理，管理本部门数据'),
('EMPLOYEE', '员工', 'employee', 50, 'SELF', '普通员工，只能查看和操作自己的数据'),
('VIEWER', '只读用户', 'viewer', 99, 'SELF', '只读用户，仅查看权限');

-- ============================================
-- 2. 添加测试用户
-- ============================================
-- admin 已在 init_data.sql 中创建，ID=1
INSERT INTO `sys_user` (`username`, `password`, `email`, `phone`, `status`) VALUES
('manager', '$2a$10$SG7HkhWRQvmDzCG3S.aYsOxxezisRSSm.jRm4uXszZMdpf0g3JuMO', 'manager@example.com', '13800138001', 1),
('user1', '$2a$10$SG7HkhWRQvmDzCG3S.aYsOxxezisRSSm.jRm4uXszZMdpf0g3JuMO', 'user1@example.com', '13800138002', 1),
('user2', '$2a$10$SG7HkhWRQvmDzCG3S.aYsOxxezisRSSm.jRm4uXszZMdpf0g3JuMO', 'user2@example.com', '13800138003', 1),
('user3', '$2a$10$SG7HkhWRQvmDzCG3S.aYsOxxezisRSSm.jRm4uXszZMdpf0g3JuMO', 'user3@example.com', '13800138004', 1),
('viewer', '$2a$10$SG7HkhWRQvmDzCG3S.aYsOxxezisRSSm.jRm4uXszZMdpf0g3JuMO', 'viewer@example.com', '13800138005', 1),
('disabled_user', '$2a$10$SG7HkhWRQvmDzCG3S.aYsOxxezisRSSm.jRm4uXszZMdpf0g3JuMO', 'disabled@example.com', '13800138006', 0);

-- ============================================
-- 3. 扩展菜单数据（添加业务模块和报表模块）
-- ============================================
-- init_data.sql 已创建系统管理模块（ID 1-19）
-- 现在添加业务模块和报表模块

-- 一级菜单：业务管理
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(0, '业务管理', 'business', 1, '/business', NULL, NULL, 'business', 2, 1, 1, '业务管理模块');

-- 二级菜单：订单管理
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(20, '订单管理', 'business-order', 1, '/business/order', 'business/order/index', NULL, 'order', 1, 1, 1, '订单管理页面');

-- 订单管理按钮权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(21, '订单查询', 'business-order-list', 2, NULL, NULL, 'order:list', NULL, 1, 0, 1, '查询订单列表'),
(21, '订单新增', 'business-order-add', 2, NULL, NULL, 'order:add', NULL, 2, 0, 1, '创建新订单'),
(21, '订单编辑', 'business-order-edit', 2, NULL, NULL, 'order:edit', NULL, 3, 0, 1, '编辑订单信息'),
(21, '订单删除', 'business-order-delete', 2, NULL, NULL, 'order:delete', NULL, 4, 0, 1, '删除订单');

-- 二级菜单：客户管理
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(20, '客户管理', 'business-customer', 1, '/business/customer', 'business/customer/index', NULL, 'customer', 2, 1, 1, '客户管理页面');

-- 客户管理按钮权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(26, '客户查询', 'business-customer-list', 2, NULL, NULL, 'customer:list', NULL, 1, 0, 1, '查询客户列表'),
(26, '客户新增', 'business-customer-add', 2, NULL, NULL, 'customer:add', NULL, 2, 0, 1, '创建新客户'),
(26, '客户编辑', 'business-customer-edit', 2, NULL, NULL, 'customer:edit', NULL, 3, 0, 1, '编辑客户信息'),
(26, '客户删除', 'business-customer-delete', 2, NULL, NULL, 'customer:delete', NULL, 4, 0, 1, '删除客户');

-- 一级菜单：报表统计
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(0, '报表统计', 'report', 1, '/report', NULL, NULL, 'chart', 3, 1, 1, '报表统计模块');

-- 二级菜单：销售报表
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(31, '销售报表', 'report-sales', 1, '/report/sales', 'report/sales/index', NULL, 'chart-line', 1, 1, 1, '销售数据统计报表');

-- 销售报表按钮权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(32, '查看报表', 'report-sales-view', 2, NULL, NULL, 'report:view', NULL, 1, 0, 1, '查看销售报表');

-- 二级菜单：用户报表
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(31, '用户报表', 'report-user', 1, '/report/user', 'report/user/index', NULL, 'chart-bar', 2, 1, 1, '用户数据统计报表');

-- 用户报表按钮权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `status`, `remark`) VALUES
(34, '查看报表', 'report-user-view', 2, NULL, NULL, 'report:view', NULL, 1, 0, 1, '查看用户报表');

-- ============================================
-- 4. 用户-角色关联数据
-- ============================================
-- admin (id=1) 已在 init_data.sql 中关联 SUPER_ADMIN (role_id=1)
-- 现在关联其他测试用户

-- manager: 部门管理员(3) + 经理(4)
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(2, 3),
(2, 4);

-- user1, user2, user3: 员工(5)
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(3, 5),
(4, 5),
(5, 5);

-- viewer: 只读用户(6)
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(6, 6);

-- ============================================
-- 5. 角色-菜单关联数据（扩展权限）
-- ============================================
-- 超级管理员(id=1)已在init_data.sql中分配所有基础菜单
-- 现在给超级管理员添加新增的业务和报表模块权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, id FROM `sys_menu` WHERE id >= 20;

-- 普通用户(id=2)保持init_data.sql中的基础查询权限不变

-- 部门管理员(id=3)：系统管理模块完整权限 + 业务管理模块完整权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 3, id FROM `sys_menu` WHERE
    id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19)  -- 系统管理模块
    OR id IN (20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30);                       -- 业务管理模块

-- 经理(id=4)：业务管理模块完整权限 + 报表查看权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 4, id FROM `sys_menu` WHERE
    id IN (20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30)  -- 业务管理模块
    OR id IN (31, 32, 33, 34, 35);                       -- 报表统计模块

-- 员工(id=5)：订单查询、客户查询权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(5, 20),  -- 业务管理
(5, 21),  -- 订单管理
(5, 22),  -- 订单查询
(5, 26),  -- 客户管理
(5, 27);  -- 客户查询

-- 只读用户(id=6)：只有查看权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(6, 20),  -- 业务管理
(6, 21),  -- 订单管理
(6, 22),  -- 订单查询
(6, 26),  -- 客户管理
(6, 27),  -- 客户查询
(6, 31),  -- 报表统计
(6, 32),  -- 销售报表
(6, 33),  -- 查看销售报表
(6, 34),  -- 用户报表
(6, 35);  -- 查看用户报表

-- ============================================
-- 6. 登录审计日志测试数据
-- ============================================
INSERT INTO `sys_login_log` (`username`, `login_ip`, `status`, `fail_reason`, `login_time`) VALUES
-- 成功登录记录
('admin', '192.168.1.100', 1, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
('admin', '192.168.1.100', 1, NULL, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
('manager', '192.168.1.101', 1, NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
('user1', '192.168.1.102', 1, NULL, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
('user2', '192.168.1.103', 1, NULL, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
('user3', '192.168.1.104', 1, NULL, DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
('viewer', '192.168.1.105', 1, NULL, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),

-- 失败登录记录
('user2', '192.168.1.103', 0, '密码错误', DATE_SUB(NOW(), INTERVAL 31 MINUTE)),
('unknown_user', '192.168.1.200', 0, '用户不存在', DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
('disabled_user', '192.168.1.106', 0, '账户已禁用', DATE_SUB(NOW(), INTERVAL 5 MINUTE)),

-- 模拟连续失败尝试
('user3', '192.168.1.104', 0, '密码错误', DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
('user3', '192.168.1.104', 0, '密码错误', DATE_SUB(NOW(), INTERVAL 48 MINUTE)),
('user3', '192.168.1.104', 0, '密码错误', DATE_SUB(NOW(), INTERVAL 46 MINUTE));

-- ============================================
-- 测试数据说明
-- ============================================
-- 【用户账号一览】
--   admin / 123456          - 超级管理员（ID=1，角色ID=1），拥有所有权限
--   manager / 123456        - 部门管理员+经理（ID=2，角色ID=3,4），系统管理+业务管理权限
--   user1 / 123456          - 普通员工（ID=3，角色ID=5），基本业务查询权限
--   user2 / 123456          - 普通员工（ID=4，角色ID=5），基本业务查询权限
--   user3 / 123456          - 普通员工（ID=5，角色ID=5），基本业务查询权限
--   viewer / 123456         - 只读用户（ID=6，角色ID=6），仅查看权限
--   disabled_user / 123456  - 已禁用账户（ID=7，无角色）
--
-- 【角色权限一览】
--   ID=1: SUPER_ADMIN      - 超级管理员，全部权限
--   ID=2: NORMAL_USER      - 普通用户，基础查询权限
--   ID=3: DEPT_ADMIN       - 部门管理员，系统+业务管理
--   ID=4: MANAGER          - 经理，业务管理+报表查看
--   ID=5: EMPLOYEE         - 员工，订单和客户查询
--   ID=6: VIEWER           - 只读用户，仅查看权限
--
-- 【菜单结构一览】
--   系统管理（ID=1）
--     ├── 用户管理（ID=2）
--     │   ├── 用户查询/新增/编辑/删除（ID=3,4,5,6）
--     ├── 角色管理（ID=7）
--     │   ├── 角色查询/新增/编辑/删除/强制删除（ID=8,9,10,11,12）
--     ├── 菜单管理（ID=13）
--     │   ├── 菜单查询/新增/编辑/删除（ID=14,15,16,17）
--     └── 开发工具（ID=18）
--         └── API文档导出（ID=19）
--
--   业务管理（ID=20）
--     ├── 订单管理（ID=21）
--     │   ├── 订单查询/新增/编辑/删除（ID=22,23,24,25）
--     └── 客户管理（ID=26）
--         ├── 客户查询/新增/编辑/删除（ID=27,28,29,30）
--
--   报表统计（ID=31）
--     ├── 销售报表（ID=32）
--     │   └── 查看报表（ID=33）
--     └── 用户报表（ID=34）
--         └── 查看报表（ID=35）
--
-- 【数据权限范围】
--   ALL          - 全部数据
--   DEPT_AND_SUB - 部门及子部门数据
--   DEPT         - 部门数据
--   SELF         - 仅本人数据
--
-- 【测试建议】
--   1. 登录测试：使用不同用户登录，验证权限隔离
--   2. 权限测试：验证按钮级权限控制（user:add, role:edit等）
--   3. 数据权限测试：验证不同角色的数据可见范围
--   4. 审计日志：检查登录日志记录是否完整
--   5. 锁定机制：多次错误密码登录测试账户锁定功能
