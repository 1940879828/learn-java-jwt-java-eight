# API 优化建议文档

> 基于当前 OpenAPI 规范（认证管理 / 用户管理 / 角色管理 / 菜单管理）的全面优化分析与改进建议
>
> 日期：2026-05-13
> 作用域：`/auth/**`、`/api/users/**`、`/api/roles/**`、`/api/menus/**`、`/api/doc/**`

---

## 目录

1. [概览与优先级](#1-概览与优先级)
2. [路径与 RESTful 设计](#2-路径与-restful-设计)
3. [HTTP 方法与语义](#3-http-方法与语义)
4. [请求 / 响应模型（DTO 与实体分离）](#4-请求--响应模型dto-与实体分离)
5. [统一响应与错误码](#5-统一响应与错误码)
6. [认证 / 安全设计](#6-认证--安全设计)
7. [参数校验与数据约束](#7-参数校验与数据约束)
8. [分页、过滤与排序](#8-分页过滤与排序)
9. [缺失的核心接口](#9-缺失的核心接口)
10. [枚举与字段语义](#10-枚举与字段语义)
11. [文档质量（OpenAPI 描述）](#11-文档质量openapi-描述)
12. [版本化、可观测性、限流](#12-版本化可观测性限流)
13. [`api-doc-controller` 治理](#13-api-doc-controller-治理)
14. [优化前后对比汇总表](#14-优化前后对比汇总表)
15. [改造路线图（分阶段落地）](#15-改造路线图分阶段落地)

---

## 1. 概览与优先级

| 优先级 | 类别 | 关键问题 |
|--------|------|---------|
| 🔴 P0 | 安全 | `/auth/logout` 请求体直接接收 `JwtUserDetails`（含 password 字段），泄露内部安全模型 |
| 🔴 P0 | RESTful | `assignRoles`、`assignMenus` 使用 `POST` 表达"覆盖式赋值"，语义错误，应为 `PUT` |
| 🔴 P0 | DTO 污染 | `SysRole`、`SysMenu` 数据库实体直接作为请求体，`createTime`、`createBy` 等系统字段可被客户端篡改 |
| 🟠 P1 | 路径一致性 | `/auth/**` 与 `/api/**` 前缀分裂；`/api/menus/role/{roleId}` 不符合 REST 嵌套规范 |
| 🟠 P1 | 错误响应 | 全部接口只声明 `200`，未声明 `400/401/403/404/409/500` |
| 🟠 P1 | 缺失接口 | 无用户列表/更新/删除/改密；无锁定接口（只有解锁）；无 `/auth/me`；无解除角色/菜单绑定接口 |
| 🟡 P2 | 分页 | `getAllRoles`、`getAllMenus` 无分页参数，列表会随数据膨胀 |
| 🟡 P2 | 校验 | `LoginRequest`、`SysRole`、`SysMenu` 缺少 `@NotBlank`、`@Size`、`@Pattern` 等约束 |
| 🟡 P2 | 枚举 | `menuType`、`visible`、`status`、`dataScope` 使用裸 `int/string`，缺乏明确取值 |
| 🟢 P3 | 文档 | 多数接口缺 `description`、`example`，无错误码表 |
| 🟢 P3 | 版本化 | 路径未带 `/v1`，未来不兼容变更将困难 |

---

## 2. 路径与 RESTful 设计

### 2.1 前缀统一

**问题：** 认证模块走 `/auth/**`，业务模块走 `/api/**`，前缀不统一会让网关路由、CORS、限流策略碎片化。

**建议：** 统一到 `/api/v1/**`：

```
/auth/login               →  /api/v1/auth/login
/auth/logout              →  /api/v1/auth/logout
/auth/refresh             →  /api/v1/auth/refresh
/auth/register            →  /api/v1/auth/register
/auth/unlock/{userId}     →  /api/v1/auth/users/{userId}/unlock   ← 见 2.2
```

### 2.2 嵌套资源与 URI 规范化

| 现状 | 建议 | 理由 |
|------|------|------|
| `POST /auth/unlock/{userId}` | `POST /api/v1/users/{userId}/unlock` 或 `PATCH /api/v1/users/{userId}` 配合 `{ "locked": false }` | 解锁是对"用户资源"的状态变更，不属于认证 |
| `GET /api/menus/role/{roleId}` | `GET /api/v1/roles/{roleId}/menus` | 已存在 `POST /api/roles/{roleId}/menus`，GET/POST 应共用同一 URI |
| `GET /api/users/{userId}/roles` | 保持 | ✅ 符合规范 |
| `POST /api/users/{userId}/roles` | `PUT /api/v1/users/{userId}/roles` | 见 [3. HTTP 方法](#3-http-方法与语义) |

### 2.3 路径命名一致性

- 单数 / 复数统一：现状已是复数（`roles`、`menus`、`users`），✅ 保持。
- 子资源动作（unlock、reset-password）建议作为**子路径动词**：`POST /users/{id}/unlock`、`POST /users/{id}/password:reset`。

---

## 3. HTTP 方法与语义

### 3.1 「覆盖式赋值」必须用 `PUT`

```
现状: POST /api/roles/{roleId}/menus    body: { menuIds: [1,2,3] }
现状: POST /api/users/{userId}/roles    body: { roleIds: [4,5] }
```

`POST` 语义上是"追加 / 创建新资源"，但根据 `AssignMenusRequest` 看，业务实际是**全量替换**该用户/角色的关联集合。这种语义应使用 `PUT`：

```
PUT /api/v1/roles/{roleId}/menus    body: { menuIds: [1,2,3] }   ← 替换
PUT /api/v1/users/{userId}/roles    body: { roleIds: [4,5] }     ← 替换
```

如果未来需要支持「增量追加」「移除单个」，再增加：

```
POST   /api/v1/users/{userId}/roles            ← 追加（幂等需用 If-Match）
DELETE /api/v1/users/{userId}/roles/{roleId}   ← 解绑单个
```

### 3.2 删除应区分软硬删除

```
DELETE /api/roles/{id}    ← 当前返回 ResultVoid，是软删还是硬删？文档未说明
```

**建议：**
- 在 OpenAPI `description` 中明确说明是软删（`status=0`）还是物理删除。
- 软删除应允许通过 `?force=true` 切换为物理删除（仅超管）。
- 删除前已被引用的资源（角色已分配给用户、菜单已分配给角色）应返回 `409 Conflict`，而不是静默级联或失败。

### 3.3 状态码规范

| 方法 | 成功状态码 | 当前 |
|------|-----------|------|
| `GET` 单个存在 | `200` | ✅ |
| `GET` 单个不存在 | `404` | ❌ 未声明 |
| `POST` 创建成功 | `201 Created` + `Location` | ❌ 返回 `200` |
| `PUT` 更新成功 | `200` 或 `204` | ✅ |
| `DELETE` 删除成功 | `204 No Content` | ❌ 返回 `200 + ResultVoid` |

> 团队若坚持「业务统一用 200 + Result 包装」，至少要在文档中明确这是**约定**，不要在 Swagger 中漏掉 `400/401/403/404/409` 的错误响应 schema。

---

## 4. 请求 / 响应模型（DTO 与实体分离）

### 4.1 实体不应直接做请求体

```jsonc
// 当前 createRole / updateRole 的 requestBody 直接是 SysRole
{
  "id": 1,
  "roleCode": "ADMIN",
  "roleName": "管理员",
  "permission": "...",
  "level": 1,
  "dataScope": "ALL",
  "createBy": "hacker",       // ❌ 客户端可伪造
  "createTime": "1970-01-01", // ❌ 客户端可伪造
  "remark": "..."
}
```

**问题：**

1. `createBy` / `createTime` 应由后端从 Token 与 `LocalDateTime.now()` 填充，禁止前端传入。
2. `id` 在 `POST /api/roles` 中无意义，但 schema 允许传入。
3. `updateRole` 路径已有 `{id}`，请求体里的 `id` 容易与路径冲突。

**建议：**

```java
// 创建
public class RoleCreateRequest {
    @NotBlank @Size(max = 32) @Pattern(regexp = "^[A-Z_]+$")
    private String roleCode;

    @NotBlank @Size(max = 32)
    private String roleName;

    @Min(0) @Max(9)
    private Integer level;

    private DataScopeEnum dataScope;

    @Size(max = 255)
    private String remark;
    // 不包含 id / createBy / createTime
}

// 更新（部分字段可选，配合 PATCH 时全部可空）
public class RoleUpdateRequest {
    @Size(max = 32)
    private String roleName;
    @Min(0) @Max(9)
    private Integer level;
    private DataScopeEnum dataScope;
    private String remark;
    // 不允许修改 roleCode（业务唯一键）
}

// 响应
public class RoleResponse {
    private Integer id;
    private String roleCode;
    private String roleName;
    private Integer level;
    private DataScopeEnum dataScope;
    private String createBy;
    private OffsetDateTime createTime;  // 用 OffsetDateTime / ISO-8601
    private String remark;
}
```

同样规则适用于 `SysMenu`、`SysUser` 全部接口。

### 4.2 `JwtUserDetails` 严禁出现在 API 中

```jsonc
// /auth/logout 当前的 requestBody —— 这是个严重问题
{
  "userId": 1,
  "username": "admin",
  "password": "BCrypt 哈希",     // 🚨 内部字段
  "authorities": [...],          // 🚨 Spring Security 内部模型
  "accountNonLocked": true,
  "accountNonExpired": true,
  "credentialsNonExpired": true
}
```

**问题：**
1. `JwtUserDetails` 是 `org.springframework.security.core.userdetails.UserDetails` 的实现，是**内部对象**。
2. 暴露了 `password`（即使是哈希），违反最小权限原则。
3. 登出根本不需要请求体——从 `Authorization: Bearer xxx` 取 token 即可。

**建议：**

```
POST /api/v1/auth/logout
Headers: Authorization: Bearer <accessToken>
Body: (none)
→ 204 No Content
```

在服务端：从 SecurityContext 拿 userId，删除 Redis 中的 refreshToken，将 accessToken 加入黑名单（带 TTL = token 剩余有效期）。

---

## 5. 统一响应与错误码

### 5.1 现状

```jsonc
{ "code": 200, "message": "ok", "data": {...} }
```

✅ 整体结构合理。但当前生成的 `ResultSysRole`、`ResultListSysMenu`、`ResultVoid` 等是**类型擦除式的多个独立 schema**，导致：

- Swagger 文档膨胀（每种 data 类型生成一个 ResultXxx）。
- 客户端代码生成器产出大量重复类。

**建议：** 使用 OpenAPI 的泛型支持（`x-generic-type` / `discriminator`），或在生成器层配置 `Result<T>`，前端 TS 接口只需一个 `ApiResult<T>`。

### 5.2 错误码表（建议补充到文档）

| HTTP | code | 含义 | 何时返回 |
|------|------|------|---------|
| 400 | 40000 | 参数校验失败 | `@Valid` 失败 |
| 401 | 40100 | 未登录 / Token 无效 | 无 Token、过期 |
| 401 | 40101 | Refresh Token 无效 | refresh 接口 |
| 403 | 40300 | 无权限 | 通过认证但无对应权限 |
| 404 | 40400 | 资源不存在 | `getRoleById` 找不到 |
| 409 | 40901 | 资源已存在 | `roleCode` 重复 |
| 409 | 40902 | 资源被引用 | 删除已被分配的角色 |
| 423 | 42300 | 账户被锁定 | 登录失败次数超限 |
| 429 | 42900 | 限流 | 登录、注册接口 |
| 500 | 50000 | 服务器内部错误 | 兜底 |

每个错误响应 schema 推荐：

```jsonc
{
  "code": 40400,
  "message": "角色不存在",
  "data": null,
  "traceId": "abc123",     // 关联日志
  "timestamp": "2026-05-13T10:00:00Z"
}
```

### 5.3 在 OpenAPI 中声明错误响应

每个接口应至少声明：

```yaml
responses:
  '200': { ... }
  '400': { $ref: '#/components/responses/BadRequest' }
  '401': { $ref: '#/components/responses/Unauthorized' }
  '403': { $ref: '#/components/responses/Forbidden' }
  '404': { $ref: '#/components/responses/NotFound' }  # 仅 GET/PUT/DELETE {id}
```

通过 `components/responses` 复用，避免重复定义。

---

## 6. 认证 / 安全设计

### 6.1 Refresh Token 存储与轮换

**当前：** `RefreshRequest` 通过 JSON Body 传 refresh token。
**风险：** 若前端把 refresh token 存在 localStorage，会被 XSS 窃取。

**建议（择一）：**
- **HttpOnly + Secure + SameSite=Strict Cookie** 存 refresh token，access token 仍在内存中。
- 接口契约保持兼容：服务端从 Cookie 优先读取，回退到 Body。
- **Refresh Token Rotation**：每次刷新都签发新的 refresh token 并失效旧的（已有此意图，文档需明确"旧 token 立即失效"）。

### 6.2 登录限流与锁定

文档提到 `unlockUser`，意味着已有"失败次数 → 锁定"机制，但没有 `lockUser` 接口（管理员主动封号）。

**建议补充：**
```
POST /api/v1/users/{userId}/lock     body: { "reason": "..." }
POST /api/v1/users/{userId}/unlock
GET  /api/v1/users/{userId}/login-attempts   ← 审计
```

登录接口应在文档中说明：
- 失败 N 次锁定 M 分钟
- 是否对同 IP / 同设备做额外限流

### 6.3 注册接口的额外保护

`/auth/register` 当前完全开放。建议：
- 邮箱 / 手机号验证（增加 `email` / `phone` 字段并要求验证码）。
- 图形验证码或 hCaptcha。
- IP 级限流（如 5 次/小时）。
- 在文档中说明：用户名是否区分大小写、保留字列表（admin、root、system）。

### 6.4 权限矩阵文档化

每个接口应在 `description` 中标注所需权限：

| 接口 | 所需权限 |
|------|---------|
| `POST /api/v1/auth/login` | 公开 |
| `POST /api/v1/auth/register` | 公开 |
| `POST /api/v1/auth/refresh` | 公开（凭 refresh token） |
| `POST /api/v1/auth/logout` | 已登录 |
| `POST /api/v1/users/{id}/unlock` | `user:unlock` 或 `ROLE_ADMIN` |
| `GET /api/v1/roles` | `role:list` |
| `POST /api/v1/roles` | `role:create` |
| `PUT /api/v1/roles/{id}` | `role:update` |
| `DELETE /api/v1/roles/{id}` | `role:delete` |
| `PUT /api/v1/roles/{id}/menus` | `role:assign-menu` |
| ... | ... |

可使用 Spring Security 的 `@PreAuthorize("hasAuthority('role:create')")`，并在 SpringDoc 中通过自定义 `OperationCustomizer` 自动注入到 `description`。

---

## 7. 参数校验与数据约束

### 7.1 `LoginRequest` 缺少约束

```jsonc
// 当前
{ "username": "string", "password": "string" }
```

**建议：**

```java
public class LoginRequest {
    @NotBlank @Size(min = 3, max = 20) @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    private String username;

    @NotBlank @Size(min = 6, max = 64)
    private String password;
}
```

与 `RegisterRequest` 保持一致（注册已有约束）。

### 7.2 `SysRole` / `SysMenu` 缺少约束

新 DTO（见 §4）应包含：

| 字段 | 约束 |
|------|------|
| `roleCode` | `@NotBlank @Pattern("^[A-Z_]{2,32}$")` |
| `roleName` | `@NotBlank @Size(max=32)` |
| `level` | `@Min(0) @Max(9)` |
| `dataScope` | `@NotNull` + 枚举 |
| `menuType` | 枚举（DIR/MENU/BUTTON） |
| `path` | `@Size(max=200)` |
| `component` | `@Size(max=200)` |
| `perms` | `@Pattern("^[a-z]+:[a-z\\-]+$")`（如 `role:create`） |

### 7.3 ID 类型一致性

| 字段 | 当前类型 | 备注 |
|------|---------|------|
| `SysRole.id` | `int32` | ⚠️ 角色数量可控可保留 |
| `SysMenu.id` | `int32` | ⚠️ 同上 |
| `userId` | `int64` | ✅ 用户量大用 long |

**建议：** 全部统一为 `int64`，未来不会有迁移代价；或在文档中明确"主键类型策略"。

---

## 8. 分页、过滤与排序

### 8.1 列表接口必须支持分页

```
GET /api/roles    → 返回全量 List<SysRole>     ← 无分页
GET /api/menus    → 返回全量 List<SysMenu>     ← 无分页
```

菜单数据量小可接受，但**角色**未来可能膨胀。建议：

```
GET /api/v1/roles?page=1&size=20&sort=createTime,desc&keyword=admin
```

响应：

```jsonc
{
  "code": 200,
  "data": {
    "items": [...],
    "page": 1,
    "size": 20,
    "total": 137,
    "totalPages": 7
  }
}
```

为兼容菜单"树状一次性加载"的场景，提供专用：

```
GET /api/v1/menus/tree    ← 显式返回树结构（无分页）
GET /api/v1/menus         ← 平铺 + 分页（管理后台表格）
```

### 8.2 过滤参数

| 接口 | 建议过滤参数 |
|------|------------|
| `GET /api/v1/roles` | `keyword`、`level`、`dataScope`、`status` |
| `GET /api/v1/menus` | `keyword`、`menuType`、`visible`、`status`、`parentId` |
| `GET /api/v1/users` | `keyword`、`status`、`locked`、`roleId` |

---

## 9. 缺失的核心接口

当前只有「用户管理」标签，但实际上**没有任何用户 CRUD 接口**（只有"查用户的角色"、"为用户分配角色"、"解锁用户"）。建议补齐：

### 9.1 用户管理

```
GET    /api/v1/users                       # 列表（分页 + 过滤）
GET    /api/v1/users/{id}                  # 详情
POST   /api/v1/users                       # 管理员创建用户（与 /register 不同）
PUT    /api/v1/users/{id}                  # 更新基本信息
PATCH  /api/v1/users/{id}                  # 部分更新（启用/禁用）
DELETE /api/v1/users/{id}                  # 软删除
POST   /api/v1/users/{id}/lock             # 锁定
POST   /api/v1/users/{id}/unlock           # 解锁（已存在，迁移路径）
POST   /api/v1/users/{id}/password:reset   # 管理员重置密码
PUT    /api/v1/users/me/password           # 自己改密
GET    /api/v1/users/me                    # 当前登录用户信息
```

### 9.2 关联管理（解绑接口缺失）

```
GET    /api/v1/users/{userId}/roles                ← 已有
PUT    /api/v1/users/{userId}/roles                ← 全量替换（原 POST 改 PUT）
POST   /api/v1/users/{userId}/roles                ← 追加单个/多个
DELETE /api/v1/users/{userId}/roles/{roleId}       ← 解绑单个（缺失）

GET    /api/v1/roles/{roleId}/menus                ← 缺失（应替换 /api/menus/role/{roleId}）
PUT    /api/v1/roles/{roleId}/menus                ← 全量替换（原 POST 改 PUT）
DELETE /api/v1/roles/{roleId}/menus/{menuId}       ← 解绑单个（缺失）

GET    /api/v1/roles/{roleId}/users                ← 反查：哪些用户拥有此角色（缺失）
GET    /api/v1/menus/{menuId}/roles                ← 反查：哪些角色拥有此菜单（缺失）
```

### 9.3 当前用户上下文

`GET /api/v1/auth/me` 或 `GET /api/v1/users/me` 是前端登录后立即需要的：
- 返回基本资料 + 角色 + 权限 + 菜单树（前端构建侧边栏）。
- 现在前端只能拼接多次请求，影响首屏。

```jsonc
GET /api/v1/users/me
{
  "id": 1,
  "username": "admin",
  "roles": [{"id":1,"roleCode":"ADMIN",...}],
  "permissions": ["role:create", "menu:list", ...],
  "menuTree": [...]
}
```

---

## 10. 枚举与字段语义

| 字段 | 当前类型 | 实际含义（推测） | 建议 |
|------|---------|----------------|------|
| `SysMenu.menuType` | `int32` | 1=目录 2=菜单 3=按钮 | 枚举 `DIR/MENU/BUTTON` |
| `SysMenu.visible` | `int32` | 0=隐藏 1=显示 | `boolean` 或枚举 |
| `SysMenu.status` | `int32` | 0=停用 1=启用 | 枚举 `DISABLED/ENABLED` |
| `SysRole.dataScope` | `string` | ALL / DEPT / SELF / CUSTOM | 显式枚举并在 schema 中 `enum: [ALL, DEPT, ...]` |
| `LoginResponse.tokenType` | `string` | 总是 "Bearer" | 可移除（前端写死） |

OpenAPI 写法：

```yaml
menuType:
  type: string
  enum: [DIR, MENU, BUTTON]
  description: 菜单类型：目录 / 菜单 / 按钮
```

---

## 11. 文档质量（OpenAPI 描述）

### 11.1 摘要与描述

许多接口只有 `summary`，没有 `description`。建议每个接口至少包含：

- 业务说明（做什么）
- 权限要求
- 主要校验规则
- 典型错误码
- 注意事项（幂等性、副作用）

示例：

```yaml
/auth/refresh:
  post:
    summary: 刷新Token
    description: |
      使用 Refresh Token 换取新的 Access Token 与 Refresh Token。
      
      - 旧 Refresh Token **立即失效**（Token Rotation）
      - Refresh Token 默认有效期 30 天；连续刷新不会延长，仅在主动登录时重置
      - 单用户同时有效 Refresh Token 数量上限：3（最早的会被踢出）
      
      错误：
      - `401 40101` Refresh Token 已失效或被撤销
      - `429 42900` 刷新频率超限（10 次/分钟）
```

### 11.2 示例值

所有 `requestBody` 和 `response` 应提供完整的 `example`：

```yaml
LoginRequest:
  example:
    username: admin
    password: "Admin@1234"
```

### 11.3 字段描述

`SysRole`、`SysMenu` 的字段几乎全部没有 `description`。应补全：

```yaml
SysRole:
  properties:
    roleCode:
      type: string
      description: 角色编码，大写字母+下划线，全局唯一，创建后不可修改
      example: ROLE_ADMIN
    level:
      type: integer
      description: 角色等级，0 最高，数值越大权限越低；分配下级时 level 必须严格大于自身
      example: 1
```

---

## 12. 版本化、可观测性、限流

### 12.1 版本化

引入 `/api/v1/**`。版本变更策略：
- 不兼容变更 → 升 major（`/v2`）。
- 新增字段、新增可选参数 → 同版本内允许。
- 在 OpenAPI `info.version` 中标注 `1.0.0`，并维护 `CHANGELOG.md`。

### 12.2 可观测性

所有响应建议附加：
- `X-Trace-Id` 响应头，贯穿日志与下游。
- 错误响应 body 中也带 `traceId`，便于用户上报问题时关联日志。

### 12.3 限流策略

在文档中明确限流规则：

| 接口 | 限流 |
|------|------|
| `/api/v1/auth/login` | 10 次/分钟/IP；5 次失败锁 15 分钟 |
| `/api/v1/auth/register` | 3 次/小时/IP |
| `/api/v1/auth/refresh` | 10 次/分钟/用户 |
| 其他 | 默认 100 次/分钟/用户 |

超限返回 `429`，响应头带 `Retry-After`。

---

## 13. `api-doc-controller` 治理

```
GET /api/doc/schemas
GET /api/doc/schemas-simple
GET /api/doc/full
```

**问题：**
1. 没有 `tags`（只有自动生成的 `api-doc-controller`），文档中归类混乱。
2. 没有 `summary`、`description`。
3. **生产环境必须关闭**——这是泄露内部 schema 的入口，黑客可借此枚举所有 DTO 结构。

**建议：**
1. 仅在 `dev`/`test` 环境注册：使用 `@Profile({"dev","test"})` 或 `@ConditionalOnProperty(name="app.dev-tools.enabled")`。
2. 增加 `tags: [开发工具]`、`description: 仅供前端在本地查看 schema，生产环境关闭`。
3. 加上 `@SecurityRequirement` 要求超管权限。

---

## 14. 优化前后对比汇总表

| 维度 | 优化前 | 优化后 |
|------|--------|--------|
| 路径前缀 | `/auth/**` + `/api/**` | `/api/v1/**` 统一 |
| 关联赋值方法 | `POST` | `PUT`（全量）/ `POST`（追加）/ `DELETE`（解绑） |
| 请求/响应模型 | 直接用实体（`SysRole`） | 拆 `*Request` / `*Response` DTO |
| 登出请求体 | `JwtUserDetails`（含 password） | 无 body，仅凭 Authorization header |
| 错误响应 | 仅 `200` | `400/401/403/404/409/423/429/500` 全声明 |
| 列表接口 | 返回全量数组 | 分页 + 过滤 + 排序 |
| 枚举字段 | 裸 `int / string` | OpenAPI `enum` 显式枚举 |
| 校验 | 仅 `RegisterRequest` 有 | 所有写接口启用 `@Valid` |
| 缺失接口 | 无用户 CRUD、无 `/me`、无解绑 | 全部补齐 |
| 文档 | 多数无 description / example | 业务说明 + 权限 + 错误码 + 示例 |
| 版本化 | 无 | `/v1`，CHANGELOG 维护 |
| 限流 | 未文档化 | 每类接口明确策略 |
| 开发接口 | `/api/doc/**` 全环境暴露 | 仅 dev/test 注册 + 权限保护 |

---

## 15. 改造路线图（分阶段落地）

> 兼容性优先，采用**新旧并存 + 弃用窗口**策略。

### 阶段 1（1-2 天）：止血与文档补强（不破坏兼容）

- [x] 在 `/auth/logout` 中**忽略**请求体的 `JwtUserDetails`，从 token 取用户；`requestBody` 标记 `deprecated`，仅保留空对象兼容。
- [x] 补全所有接口的 `400/401/403/404` 错误响应声明（不改实际行为，只补文档）。
- [x] `LoginRequest` 加 `@NotBlank`、`@Size`。
- [x] `api-doc-controller` 增加 `@Profile`，生产关闭。
- [x] OpenAPI 字段补 `description` 与 `example`。

### 阶段 2（3-5 天）：DTO 拆分与方法语义修正

- [ ] 引入 `RoleCreateRequest`、`RoleUpdateRequest`、`RoleResponse`，老接口保留并标 `deprecated`。
- [ ] 同样改造 `Menu` 系列。
- [ ] 新增 `PUT /api/v1/users/{userId}/roles`、`PUT /api/v1/roles/{roleId}/menus`，旧 `POST` 标记 `deprecated`，3 个月后下线。
- [ ] 移动 `/api/menus/role/{roleId}` → `/api/v1/roles/{roleId}/menus`（GET）。

### 阶段 3（1-2 周）：补齐缺失接口

- [ ] 用户管理完整 CRUD。
- [ ] `GET /api/v1/users/me`、`PUT /api/v1/users/me/password`。
- [ ] 解绑类接口、反查类接口。
- [ ] 列表分页 + 过滤 + 排序。

### 阶段 4（持续）：体系化

- [ ] 全部走 `/api/v1/`，旧路径 301 / 410。
- [ ] Refresh Token 改 HttpOnly Cookie + Rotation。
- [ ] 接口级权限注解 + 自动写入 OpenAPI description。
- [ ] 限流策略接入网关，文档化。

---

## 附录 A：建议引入的通用 OpenAPI 组件

```yaml
components:
  responses:
    BadRequest:
      description: 参数校验失败
      content:
        application/json:
          schema: { $ref: '#/components/schemas/ErrorResponse' }
          example:
            code: 40000
            message: "参数 username 不能为空"
            data: null
            traceId: "abc-123"

    Unauthorized:
      description: 未登录或 Token 无效
      content:
        application/json:
          schema: { $ref: '#/components/schemas/ErrorResponse' }

    Forbidden:
      description: 权限不足
      content:
        application/json:
          schema: { $ref: '#/components/schemas/ErrorResponse' }

    NotFound:
      description: 资源不存在
      content:
        application/json:
          schema: { $ref: '#/components/schemas/ErrorResponse' }

  schemas:
    ErrorResponse:
      type: object
      properties:
        code:      { type: integer, example: 40400 }
        message:   { type: string,  example: "角色不存在" }
        data:      { type: object,  nullable: true }
        traceId:   { type: string,  example: "abc-123" }
        timestamp: { type: string, format: date-time }

    PageResult:
      type: object
      properties:
        items:      { type: array, items: { type: object } }
        page:       { type: integer, example: 1 }
        size:       { type: integer, example: 20 }
        total:      { type: integer, example: 137 }
        totalPages: { type: integer, example: 7 }
```

---

## 附录 B：参考资料

- [RFC 7231 - HTTP/1.1 Semantics](https://tools.ietf.org/html/rfc7231)
- [Microsoft REST API Guidelines](https://github.com/microsoft/api-guidelines/blob/vNext/Guidelines.md)
- [Google AIP-126 Enumerations](https://google.aip.dev/126)
- [OWASP API Security Top 10 (2023)](https://owasp.org/API-Security/editions/2023/en/0x11-t10/)
- [Spring Security Reference — Token Rotation](https://docs.spring.io/spring-security/reference/)
