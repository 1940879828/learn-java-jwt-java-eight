# Admin权限配置解决方案

## 问题原因

权限信息存储在JWT token中，修改数据库权限后需要**重新登录**才能刷新token中的权限。

## 权限加载流程

```
登录 → UserDetailsServiceImpl.loadUserByUsername()
     → userMapper.findPermissionsByUserId()
     → 权限列表 → JWT token (authorities字段)
     → 后续请求使用token中的权限验证
```

## 解决方案

### 方案1: 重新登录（推荐）

1. **重新执行SQL**（如果还没执行）
   ```bash
   mysql -u root -p jwt_java_eight < D:\Project\Learn\learn-java-jwt-java-eight\docs\sql\test_menu_data.sql
   ```

2. **退出当前登录并重新登录**
   - 这会生成新的JWT token
   - 新token包含最新的权限数据

### 方案2: 验证数据库权限配置

执行以下SQL检查admin的权限配置：

```sql
USE jwt_java_eight;

-- 1. 检查admin用户
SELECT * FROM sys_user WHERE username = 'admin';
-- 应该看到: id=1, username=admin, status=1

-- 2. 检查admin的角色
SELECT u.username, r.* 
FROM sys_user u
JOIN sys_user_role ur ON u.id = ur.user_id
JOIN sys_role r ON ur.role_id = r.id
WHERE u.username = 'admin';
-- 应该看到: role_code=SUPER_ADMIN

-- 3. 检查SUPER_ADMIN角色的菜单权限数量
SELECT COUNT(*) as permission_count
FROM sys_role_menu
WHERE role_id = 1;
-- 应该等于菜单总数（25个）

-- 4. 检查admin的所有权限
SELECT DISTINCT m.perms
FROM sys_menu m
JOIN sys_role_menu rm ON m.id = rm.menu_id
JOIN sys_user_role ur ON rm.role_id = ur.role_id
WHERE ur.user_id = 1 AND m.perms IS NOT NULL
ORDER BY m.perms;
-- 应该看到所有权限：menu:list, menu:add, menu:edit, menu:delete, 等等
```

### 方案3: 手动补充权限（如果数据不完整）

如果发现权限数据不完整，执行：

```sql
-- 确保admin用户关联到SUPER_ADMIN角色
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- 确保SUPER_ADMIN拥有所有菜单权限
DELETE FROM `sys_role_menu` WHERE role_id = 1;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, id FROM `sys_menu`;
```

## 权限验证

登录后，检查返回的用户信息中的permissions数组：

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "admin",
    "permissions": [
      "user:list",
      "user:add",
      "user:edit",
      "user:delete",
      "user:lock",
      "user:unlock",
      "user:assign-role",
      "role:list",
      "role:add",
      "role:edit",
      "role:delete",
      "role:force-delete",
      "role:assign-menu",
      "menu:list",
      "menu:add",
      "menu:edit",
      "menu:delete",
      "system:dev-tools"
    ]
  }
}
```

如果permissions数组为空或不完整，说明数据库配置有问题，需要检查方案2或执行方案3。

## 常见接口权限要求

| 接口 | 需要的权限 |
|------|-----------|
| GET /api/v1/menus | `menu:list` |
| GET /api/v1/menus/tree | `menu:list` |
| GET /api/v1/menus/{id} | `menu:list` |
| POST /api/v1/menus | `menu:add` |
| PUT /api/v1/menus/{id} | `menu:edit` |
| DELETE /api/v1/menus/{id} | `menu:delete` |

## 调试日志

查看日志确认权限加载：

```
[UserDetails] 加载用户: admin
[UserDetails] 用户 admin 拥有 1 个角色
[UserDetails] 用户 admin 拥有 18 个权限
[UserDetails] 用户加载成功: admin, 权限总数: 19
```

如果权限数量不对，说明：
1. 数据库菜单数据不完整
2. sys_role_menu关联数据不正确
3. sys_user_role关联数据不正确

## 快速测试

```bash
# 1. 登录获取token
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 2. 获取当前用户信息（查看permissions）
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# 3. 测试菜单访问
curl -X GET http://localhost:8080/api/v1/menus/tree \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

如果第3步返回403权限不足，检查第2步的permissions数组。
