# API 改进提示词

> 用法：把下面 `===== PROMPT START =====` 到 `===== PROMPT END =====` 之间的内容**整段**粘贴给 Claude / Cursor / 任何 AI 编程助手。
> 提示词已自包含——不依赖本次会话上下文。

---

===== PROMPT START =====

你是一名熟悉 Spring Boot 2.x + Spring Security + MyBatis 的资深 Java 后端工程师。我有一个 Java 8 + Spring Boot 的 RBAC 后端项目（路径：`D:\Project\Learn\learn-java-jwt-java-eight`），刚完成了 27 个 commit 的 API 重构，但仍残留 **1 个高危安全漏洞** 和 **6 个明确缺陷**。请你按下面的清单逐项修复，**严格遵守约束**，全部完成后输出验证报告。

## 项目背景（30 秒读完）

- **包根**：`org.example.jwtjavaeight`
- **控制器路径**：`src/main/java/org/example/jwtjavaeight/controller/`
- **DTO 路径**：`src/main/java/org/example/jwtjavaeight/domain/dto/`
- **实体路径**：`src/main/java/org/example/jwtjavaeight/domain/entity/`
- **Mapper XML**：`src/main/resources/mapper/`
- **统一前缀**：`/api/v1/**`
- **响应包装**：`Result<T>`（位于 `common/Result.java`，已含 `traceId` / `timestamp` 字段）
- **错误码**：`enums/ErrorCode.java`（枚举，5 位粒度，如 `40401 USER_NOT_FOUND`）
- **权限模型**：`@PreAuthorize("hasAuthority('xxx:yyy')")`，命名规范 `{资源}:{动作}`，资源为 `user/role/menu`，动作为 `list/add/edit/delete`

## 全局约束（违反需返工）

1. **不要重命名公共方法**，除非清单明确要求。
2. **不要改动数据库 schema**。
3. **不要新加依赖**（包括 Spring Cloud Sleuth、Micrometer Tracing 等）。
4. **改完每一项就单独 commit**，commit message 格式：`fix(scope): 问题描述`（如 `fix(menu): correct authority codes from role:* to menu:*`）。
5. 不要为本次改动加 inline 注释，除非是解释非显然的安全考虑。
6. 完成所有项后，运行 `./mvnw clean compile` 确保编译通过；如有 `./mvnw test` 可跑则跑。

---

## 🔴 P0-1：菜单权限码张冠李戴（**最优先**）

**文件**：`controller/MenuController.java`

**问题**：所有方法的 `@PreAuthorize` 仍用 `role:*` 权限码，应改为 `menu:*`。该错误导致：
- 任何拥有"角色管理"权限的用户可改菜单（越权）
- 无法独立分配"菜单只读"权限给前端用户

**精确替换**（共 7 处）：

| 行号附近 | 方法 | 当前 | 目标 |
|---------|------|------|------|
| listMenus | `role:list` | `menu:list` |
| getMenuTree | `role:list` | `menu:list` |
| getMenuById | `role:list` | `menu:list` |
| createMenu | `role:add` | `menu:add` |
| updateMenu | `role:edit` | `menu:edit` |
| deleteMenu | `role:delete` | `menu:delete` |
| getMenuRoles | `role:list` | `menu:list`（这是查菜单的反向关联，按菜单权限） |

**验证**：
- `grep -n "role:" src/main/java/org/example/jwtjavaeight/controller/MenuController.java` 应无输出。
- `RoleController` 和 `UserController` 不应被改动。

---

## 🔴 P0-2：分页排序参数潜在 SQL 注入（**必须最先验证**）

**文件**：`domain/dto/PageRequest.java`（字段 `sort` / `order` 是裸 String）+ **所有** Mapper XML（`src/main/resources/mapper/*.xml`）

**步骤**：

### 第一步：巡检 Mapper XML

逐个打开 `RoleMapper.xml`、`UserMapper.xml`、`MenuMapper.xml`，搜索 `ORDER BY`。
- 如果出现 `ORDER BY ${sort}` 或 `${order}`（注意是 `$` 不是 `#`），属于 SQL 注入入口。
- 如果用了 `#{sort}`，MyBatis 会做参数绑定，问题较小但 ORDER BY 用 `#` 实际不会按预期执行（会被当成字面量），需也修正为白名单 + `${}`。

### 第二步：在 `PageRequest` 加白名单基类（子类指定可排序列）

```java
public abstract class PageRequest {
    @Min(1) private int page = 1;
    @Min(1) @Max(100) private int size = 20;
    private String sort = "id";
    private String order = "asc";

    /** 子类指定允许排序的列集合 */
    protected abstract java.util.Set<String> allowedSortColumns();

    public String getSafeSort() {
        return allowedSortColumns().contains(sort) ? sort : "id";
    }

    public String getSafeOrder() {
        return "desc".equalsIgnoreCase(order) ? "desc" : "asc";
    }

    public int getOffset() { return (page - 1) * size; }
    // ... getters/setters
}
```

`RoleQueryFilter`、`UserQueryFilter`、`MenuQueryFilter` 各自实现 `allowedSortColumns()`，例如：

```java
@Override
protected Set<String> allowedSortColumns() {
    return Set.of("id", "role_code", "role_name", "level", "create_time");
}
```

### 第三步：Mapper XML 改用 `getSafeSort()` / `getSafeOrder()`

```xml
ORDER BY ${safeSort} ${safeOrder}
```

并确保 Mapper 接口里参数能取到（如 `@Param("safeSort") String safeSort` 或直接传 Filter 对象后用 `#{filter.safeSort}` 不行——`$` 要求 OGNL 路径，需写 `${filter.safeSort}`）。

**验证**：
- 手工尝试发送 `?sort=id;DROP TABLE sys_user--&order=desc`，期望降级为 `id asc`。
- `grep -rn '\${sort\|\${order' src/main/resources/mapper/` 应无输出（或只剩 safeSort/safeOrder）。

---

## 🟠 P1-3：返回类型残留实体（DTO 反向泄露）

**两个位置必须改**：

### 3a. `RoleController.getRoleMenus`

**文件**：`controller/RoleController.java` 的 `getRoleMenus` 方法

**当前**：返回 `ResponseEntity<Result<List<SysMenu>>>`
**目标**：返回 `ResponseEntity<Result<List<MenuResponse>>>`

`roleService.findMenusByRoleId(id)` 需相应改为返回 `List<MenuResponse>`（在 Service 层做实体→DTO 转换；若已有 MenuMapper 工具类则复用）。

### 3b. `UserController.getUserRoles`

**文件**：`controller/UserController.java` 的 `getUserRoles` 方法

**当前**：返回 `ResponseEntity<Result<List<SysRole>>>`
**目标**：返回 `ResponseEntity<Result<List<RoleResponse>>>`

同样在 Service 层做转换。

**验证**：
- `grep -n "List<Sys" src/main/java/org/example/jwtjavaeight/controller/*.java` 应只剩下确实需要返回实体的内部接口（理想情况：空）。

---

## 🟠 P1-4：`traceId` 永远为 null（排障失效）

**问题**：`Result<T>` 通过 `MDC.get("traceId")` 取值，但项目内**没有 Filter 写入 traceId**。

**修复**：新建 `security/TraceIdFilter.java`：

```java
package org.example.jwtjavaeight.security;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {
    private static final String TRACE_ID = "traceId";
    private static final String HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String traceId = Optional.ofNullable(req.getHeader(HEADER))
                .filter(s -> !s.isEmpty())
                .orElseGet(() -> UUID.randomUUID().toString().replace("-", ""));
        MDC.put(TRACE_ID, traceId);
        res.setHeader(HEADER, traceId);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }
}
```

**验证**：启动后调用任意接口，响应应带 `X-Trace-Id` 头，且响应 body 中的 `traceId` 字段非空、与 header 一致。

---

## 🟠 P1-5：菜单 ID 类型不一致

**问题**：
- `MenuController.@PathVariable Integer id` / `Integer parentId`（实体 `SysMenu.id` 为 `Integer`）
- 但 `MenuCreateRequest.parentId` 是 `Long`

**修复方向**：保持与实体一致，将 `MenuCreateRequest.parentId` 和 `MenuUpdateRequest.parentId` 改为 `Integer`。若涉及 Service / Mapper 签名也要同步。

**验证**：`grep -n "Long.*parentId\|parentId.*Long" src/main/java/org/example/jwtjavaeight/` 应仅在数据库相关迁移脚本中出现（如果有），Java 代码内统一为 `Integer`。

---

## 🟠 P1-6：`ApiDocController` 三连风险

**文件**：`controller/ApiDocController.java`

修复要点：

1. **类级别加 `@Profile("!prod")`**：

```java
import org.springframework.context.annotation.Profile;

@Profile("!prod")
@RestController
@RequestMapping("/api/doc")
@Tag(name = "开发工具", description = "仅限非生产环境")
public class ApiDocController { ... }
```

2. **端口从配置读取**，构造器注入：

```java
@Value("${server.port:8080}")
private int serverPort;

private JsonNode getOpenApiDoc() throws Exception {
    String apiDocsUrl = "http://localhost:" + serverPort + "/v3/api-docs";
    ...
}
```

3. **三个 `@GetMapping` 方法都加 `@PreAuthorize`**：

```java
@PreAuthorize("hasAuthority('system:dev-tools')")
```

**验证**：在 `application.yml` 中临时设 `spring.profiles.active: prod` 启动，三个 `/api/doc/**` 接口应返回 404（Bean 不注册）。

---

## 🟢 P2-7：`force=true` 删除角色需更高权限

**文件**：`controller/RoleController.java` 的 `deleteRole` 方法

**当前**：`@PreAuthorize("hasAuthority('role:delete')")`

**修复**：拆为方法级 SpEL，按参数动态判断：

```java
@PreAuthorize(
    "(#force == false and hasAuthority('role:delete')) " +
    "or (#force == true and hasAuthority('role:force-delete'))"
)
```

**验证**：用只有 `role:delete` 权限的 token 调 `DELETE /api/v1/roles/1?force=true`，应返回 403。

---

## 输出要求

完成全部 7 项后，请输出：

1. **修改文件清单**（按 commit 顺序）。
2. **每项的验证结果**（通过 / 未通过 + 原因）。
3. **额外发现**：如果在巡检 Mapper XML（P0-2）时发现其他 SQL 注入入口（如 `LIKE '%${keyword}%'`、`WHERE ${...}`），一并列出，**不要自作主张修复**——告诉我位置即可。
4. **未完成项**：如果某项因依赖缺失或语义不清未完成，说明阻碍并给出建议方案。

不要输出诸如"这些改动让代码更加健壮"之类的客套话。直接给清单。

===== PROMPT END =====

---

## 使用建议

- 把 P0-2（SQL 注入巡检）放第一位是刻意安排——一旦发现 `${sort}` 漏洞，应立即停下其他工作，确认数据库未被攻破后再继续。
- 如果你想分多轮跑，P0-1 / P0-2 一定要在第一轮；P1 项可以分轮；P2 可独立处理。
- 修复完跑一遍 `git log --oneline -10` 检查 commit 颗粒度是否合理（每项一个 commit）。
- 修复完建议再用本文档目录下 [API优化建议.md](./API优化建议.md) 的"阶段 4：体系化"章节做下一步规划（限流、Refresh Token Rotation、HttpOnly Cookie 等）。
