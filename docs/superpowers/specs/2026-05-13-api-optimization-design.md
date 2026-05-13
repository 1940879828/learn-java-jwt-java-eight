# API 优化设计文档

**日期**: 2026-05-13  
**作者**: Claude Sonnet 4.5  
**状态**: 待审查  
**优化范围**: 全部 P0-P3 优化（完整路线图）

---

## 1. 概述

基于 `docs/API优化建议.md` 的全面分析，本设计将对现有 JWT 认证系统的 API 进行全方位优化，执行完整的 4 阶段路线图。项目为教学性质，无需考虑向后兼容，直接替换为新版本。

### 1.1 优化目标

- **安全加固**: 修复 P0 级安全漏洞（JwtUserDetails 泄露、DTO 污染）
- **RESTful 规范**: 统一路径、修正 HTTP 方法语义
- **完整功能**: 补齐用户 CRUD、分页、过滤、解绑等缺失接口
- **工程质量**: 建立错误码体系、校验规范、测试覆盖

### 1.2 技术栈

- Spring Boot 2.7.6
- Spring Security 5.7.5
- JWT 0.11.5
- MyBatis 2.2.2
- MySQL 5.7.44
- Java 8

---

## 2. 整体架构与版本化

### 2.1 路径体系（直接替换）

```
统一前缀: /api/v1/

/api/v1/
├── auth/                          # 认证模块
│   ├── POST   /login              # 登录
│   ├── POST   /logout             # 登出
│   ├── POST   /refresh            # 刷新token
│   └── POST   /register           # 注册
│
├── users/                         # 用户管理（完整CRUD）
│   ├── GET    /                   # 列表（分页+过滤）
│   ├── GET    /{id}               # 详情
│   ├── POST   /                   # 创建
│   ├── PUT    /{id}               # 更新
│   ├── DELETE /{id}               # 删除（软删除）
│   ├── GET    /{id}/roles         # 查询用户角色
│   ├── PUT    /{id}/roles         # 全量替换角色
│   ├── POST   /{id}/roles         # 追加角色
│   ├── DELETE /{id}/roles/{roleId} # 解绑单个角色
│   ├── POST   /{id}/lock          # 锁定用户
│   ├── POST   /{id}/unlock        # 解锁用户
│   ├── POST   /{id}/password:reset # 管理员重置密码
│   ├── GET    /me                 # 当前用户信息
│   └── PUT    /me/password        # 修改自己密码
│
├── roles/                         # 角色管理
│   ├── GET    /                   # 列表（分页+过滤）
│   ├── GET    /{id}               # 详情
│   ├── POST   /                   # 创建
│   ├── PUT    /{id}               # 更新
│   ├── DELETE /{id}               # 删除（软删除）
│   ├── GET    /{id}/menus         # 查询角色菜单
│   ├── PUT    /{id}/menus         # 全量替换菜单
│   ├── DELETE /{id}/menus/{menuId} # 解绑单个菜单
│   └── GET    /{id}/users         # 反查：拥有此角色的用户
│
└── menus/                         # 菜单管理
    ├── GET    /                   # 列表（分页+过滤）
    ├── GET    /tree               # 树形结构（无分页）
    ├── GET    /{id}               # 详情
    ├── POST   /                   # 创建
    ├── PUT    /{id}               # 更新
    ├── DELETE /{id}               # 删除（软删除）
    └── GET    /{id}/roles         # 反查：拥有此菜单的角色
```

### 2.2 Controller 层组织

- 直接重构现有 Controller（不创建 v1 子包）
- 删除 `AuthController.unlock()`，迁移到 `UserController`
- 所有接口添加 `@Operation` 完整文档

### 2.3 路径迁移映射

| 旧路径 | 新路径 | 变更说明 |
|-------|--------|---------|
| `POST /auth/unlock/{userId}` | `POST /api/v1/users/{id}/unlock` | 解锁是用户状态操作 |
| `GET /api/menus/role/{roleId}` | `GET /api/v1/roles/{id}/menus` | 嵌套资源规范化 |
| `POST /api/users/{userId}/roles` | `PUT /api/v1/users/{id}/roles` | 覆盖式赋值改用 PUT |
| `POST /api/roles/{roleId}/menus` | `PUT /api/v1/roles/{id}/menus` | 同上 |

---

## 3. DTO 层设计

### 3.1 设计原则

1. **完全隔离**: 请求/响应 DTO 与数据库实体分离
2. **系统字段保护**: `id`、`createBy`、`createTime` 禁止客户端传入
3. **操作分离**: 创建/更新使用不同 DTO
4. **校验注解**: 所有 Request 启用 `@Valid`

### 3.2 命名规范

```
{Entity}{Operation}Request/Response

示例:
- RoleCreateRequest    创建请求
- RoleUpdateRequest    更新请求
- RoleResponse         响应
- PageResponse<T>      分页响应（通用）
```

### 3.3 用户相关 DTO

```java
// 创建用户
public class UserCreateRequest {
    @NotBlank @Size(min = 3, max = 20) @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    private String username;
    
    @NotBlank @Size(min = 6, max = 64)
    private String password;
    
    @Email @NotBlank
    private String email;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$")
    private String phone;
    
    @Size(max = 255)
    private String remark;
}

// 更新用户
public class UserUpdateRequest {
    @Email
    private String email;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$")
    private String phone;
    
    @Size(max = 255)
    private String remark;
    // 不允许修改 username
}

// 用户响应
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private Integer status;
    private Boolean locked;
    private String createBy;
    private OffsetDateTime createTime;
    private String remark;
    // 不含 password
}

// 用户详情（含角色权限菜单）
public class UserDetailResponse extends UserResponse {
    private List<RoleResponse> roles;
    private List<String> permissions;
    private List<MenuTreeNode> menuTree;
}

// 修改密码
public class ChangePasswordRequest {
    @NotBlank
    private String oldPassword;
    
    @NotBlank @Size(min = 6, max = 64)
    private String newPassword;
}
```

### 3.4 角色相关 DTO

```java
// 创建角色
public class RoleCreateRequest {
    @NotBlank @Size(max = 32) @Pattern(regexp = "^[A-Z_]{2,32}$")
    private String roleCode;
    
    @NotBlank @Size(max = 32)
    private String roleName;
    
    @Min(0) @Max(9)
    private Integer level;
    
    @NotNull
    private DataScopeEnum dataScope;
    
    @Size(max = 255)
    private String remark;
}

// 更新角色
public class RoleUpdateRequest {
    @Size(max = 32)
    private String roleName;
    
    @Min(0) @Max(9)
    private Integer level;
    
    private DataScopeEnum dataScope;
    
    @Size(max = 255)
    private String remark;
    // 不允许修改 roleCode（业务唯一键）
}

// 角色响应
public class RoleResponse {
    private Long id;
    private String roleCode;
    private String roleName;
    private Integer level;
    private DataScopeEnum dataScope;
    private String createBy;
    private OffsetDateTime createTime;
    private String remark;
}
```

### 3.5 菜单相关 DTO

```java
// 创建菜单
public class MenuCreateRequest {
    private Long parentId;
    
    @NotBlank @Size(max = 32)
    private String menuName;
    
    @NotBlank @Size(max = 32) @Pattern(regexp = "^[a-z][a-z0-9\\-]*$")
    private String menuCode;
    
    @NotNull
    private MenuTypeEnum menuType;
    
    @Size(max = 200)
    private String path;
    
    @Size(max = 200)
    private String component;
    
    @Pattern(regexp = "^[a-z]+:[a-z\\-]+$")
    private String perms;  // 如 role:create
    
    @Size(max = 50)
    private String icon;
    
    @Min(0)
    private Integer sortOrder;
    
    private Boolean visible;
    
    private Integer status;
    
    @Size(max = 255)
    private String remark;
}

// 更新菜单
public class MenuUpdateRequest {
    @Size(max = 32)
    private String menuName;
    
    private MenuTypeEnum menuType;
    
    @Size(max = 200)
    private String path;
    
    @Size(max = 200)
    private String component;
    
    @Pattern(regexp = "^[a-z]+:[a-z\\-]+$")
    private String perms;
    
    @Size(max = 50)
    private String icon;
    
    @Min(0)
    private Integer sortOrder;
    
    private Boolean visible;
    
    private Integer status;
    
    @Size(max = 255)
    private String remark;
    // 不允许修改 menuCode、parentId（防止树结构混乱）
}

// 菜单响应
public class MenuResponse {
    private Long id;
    private Long parentId;
    private String menuName;
    private String menuCode;
    private MenuTypeEnum menuType;
    private String path;
    private String component;
    private String perms;
    private String icon;
    private Integer sortOrder;
    private Boolean visible;
    private Integer status;
    private String createBy;
    private OffsetDateTime createTime;
    private String remark;
}

// 菜单树节点
public class MenuTreeNode extends MenuResponse {
    private List<MenuTreeNode> children;
}
```

### 3.6 通用 DTO

```java
// 分配角色
public class AssignRolesRequest {
    @NotEmpty
    private List<Long> roleIds;
}

// 分配菜单
public class AssignMenusRequest {
    @NotEmpty
    private List<Long> menuIds;
}

// 分页响应
public class PageResponse<T> {
    private List<T> items;
    private Integer page;
    private Integer size;
    private Long total;
    private Integer totalPages;
    
    public static <T> PageResponse<T> of(List<T> items, int page, int size, long total) {
        PageResponse<T> response = new PageResponse<>();
        response.setItems(items);
        response.setPage(page);
        response.setSize(size);
        response.setTotal(total);
        response.setTotalPages((int) Math.ceil((double) total / size));
        return response;
    }
}

// 错误响应
public class ErrorResponse {
    private Integer code;
    private String message;
    private Object data;
    private String traceId;
    private OffsetDateTime timestamp;
}
```

### 3.7 枚举定义

```java
public enum DataScopeEnum {
    ALL("全部数据权限"),
    DEPT("部门数据权限"),
    DEPT_AND_SUB("部门及子部门数据权限"),
    SELF("仅本人数据权限"),
    CUSTOM("自定义数据权限");
    
    private final String description;
}

public enum MenuTypeEnum {
    DIR(1, "目录"),
    MENU(2, "菜单"),
    BUTTON(3, "按钮");
    
    private final Integer code;
    private final String description;
}
```

---

## 4. HTTP 方法与语义

### 4.1 RESTful 映射规范

| 操作 | HTTP 方法 | 路径示例 | 成功状态码 | 响应体 |
|------|----------|---------|-----------|--------|
| 列表查询 | GET | `/api/v1/roles?page=1&size=20` | 200 | `Result<PageResponse<RoleResponse>>` |
| 单个查询 | GET | `/api/v1/roles/{id}` | 200 / 404 | `Result<RoleResponse>` |
| 创建资源 | POST | `/api/v1/roles` | 201 | `Result<RoleResponse>` + `Location` header |
| 全量更新 | PUT | `/api/v1/roles/{id}` | 200 | `Result<RoleResponse>` |
| 删除资源 | DELETE | `/api/v1/roles/{id}` | 200 | `Result<Void>` |
| 全量替换关联 | PUT | `/api/v1/users/{id}/roles` | 200 | `Result<Void>` |
| 追加关联 | POST | `/api/v1/users/{id}/roles` | 200 | `Result<Void>` |
| 解绑关联 | DELETE | `/api/v1/users/{id}/roles/{roleId}` | 200 | `Result<Void>` |
| 动作型接口 | POST | `/api/v1/users/{id}/lock` | 200 | `Result<Void>` |

### 4.2 关联操作语义修正

**错误示范（现状）：**
```java
POST /api/users/{userId}/roles  body: {roleIds: [1,2,3]}
// 语义不明确：是追加还是替换？
```

**正确设计：**
```java
// 全量替换为 [1,2,3]
PUT /api/v1/users/{userId}/roles  body: {roleIds: [1,2,3]}

// 追加（幂等：已存在则忽略）
POST /api/v1/users/{userId}/roles  body: {roleIds: [4,5]}

// 解绑单个
DELETE /api/v1/users/{userId}/roles/{roleId}
```

### 4.3 创建操作返回 201 + Location

```java
@PostMapping
public ResponseEntity<Result<RoleResponse>> createRole(
    @Valid @RequestBody RoleCreateRequest request
) {
    RoleResponse response = roleService.create(request);
    URI location = URI.create("/api/v1/roles/" + response.getId());
    return ResponseEntity.created(location).body(Result.success(response));
}
```

### 4.4 删除操作规范

- **软删除**: 更新 `status=0`，返回 `200` + `Result<Void>`
- **删除前检查**: 若资源被引用 → 返回 `409 Conflict`
- **强制删除**: 支持 `?force=true` 级联删除（仅超管）

```java
@DeleteMapping("/{id}")
@PreAuthorize("hasAuthority('role:delete')")
public Result<Void> delete(
    @PathVariable Long id,
    @RequestParam(defaultValue = "false") boolean force
) {
    roleService.delete(id, force);
    return Result.success(null);
}
```

### 4.5 状态变更动作

```java
POST /api/v1/users/{id}/lock          body: {reason: "违规操作"}
POST /api/v1/users/{id}/unlock        body: (空)
POST /api/v1/users/{id}/password:reset body: {newPassword: "..."}
```

---

## 5. 错误处理体系

### 5.1 错误码枚举

```java
public enum ErrorCode {
    // 2xx 成功
    SUCCESS(200, "操作成功"),
    
    // 4xx 客户端错误
    BAD_REQUEST(40000, "请求参数错误"),
    VALIDATION_FAILED(40001, "参数校验失败"),
    
    UNAUTHORIZED(40100, "未登录或Token无效"),
    REFRESH_TOKEN_INVALID(40101, "Refresh Token无效"),
    
    FORBIDDEN(40300, "权限不足"),
    
    NOT_FOUND(40400, "资源不存在"),
    USER_NOT_FOUND(40401, "用户不存在"),
    ROLE_NOT_FOUND(40402, "角色不存在"),
    MENU_NOT_FOUND(40403, "菜单不存在"),
    
    CONFLICT(40900, "资源冲突"),
    DUPLICATE_RESOURCE(40901, "资源已存在"),
    RESOURCE_IN_USE(40902, "资源被引用，无法删除"),
    
    LOCKED(42300, "账户已被锁定"),
    
    TOO_MANY_REQUESTS(42900, "请求过于频繁"),
    
    // 5xx 服务器错误
    INTERNAL_ERROR(50000, "服务器内部错误");
    
    private final int code;
    private final String message;
}
```

### 5.2 全局异常处理器

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    // 参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.status(400)
            .body(Result.error(ErrorCode.VALIDATION_FAILED, message));
    }
    
    // 业务异常
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
            .body(Result.error(ex.getErrorCode(), ex.getMessage()));
    }
    
    // 资源不存在
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404)
            .body(Result.error(ErrorCode.NOT_FOUND, ex.getMessage()));
    }
    
    // Spring Security 认证异常
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(401)
            .body(Result.error(ErrorCode.UNAUTHORIZED, "认证失败"));
    }
    
    // Spring Security 授权异常
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccess(AccessDeniedException ex) {
        return ResponseEntity.status(403)
            .body(Result.error(ErrorCode.FORBIDDEN, "权限不足"));
    }
    
    // 兜底异常
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleGeneric(Exception ex, HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        log.error("Unexpected error [traceId={}]: ", traceId, ex);
        return ResponseEntity.status(500)
            .body(Result.error(ErrorCode.INTERNAL_ERROR, "系统异常，请联系管理员"));
    }
}
```

### 5.3 自定义业务异常

```java
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final int httpStatus;
    private String customMessage;
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getCode() / 100;
    }
    
    public static BusinessException of(ErrorCode errorCode, String customMessage) {
        BusinessException ex = new BusinessException(errorCode);
        ex.customMessage = customMessage;
        return ex;
    }
    
    @Override
    public String getMessage() {
        return customMessage != null ? customMessage : super.getMessage();
    }
}

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resourceType, Object id) {
        super(ErrorCode.NOT_FOUND);
        super.customMessage = String.format("%s[id=%s]不存在", resourceType, id);
    }
}
```

### 5.4 Result 包装类增强

```java
@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private String traceId;           // 新增
    private OffsetDateTime timestamp; // 新增
    
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        result.setTraceId(MDC.get("traceId"));
        result.setTimestamp(OffsetDateTime.now());
        return result;
    }
    
    public static <T> Result<T> error(ErrorCode errorCode, String message) {
        Result<T> result = new Result<>();
        result.setCode(errorCode.getCode());
        result.setMessage(message);
        result.setTraceId(MDC.get("traceId"));
        result.setTimestamp(OffsetDateTime.now());
        return result;
    }
}
```

### 5.5 OpenAPI 错误响应声明

```java
@Operation(
    summary = "创建角色",
    description = "需要权限：role:create",
    responses = {
        @ApiResponse(responseCode = "201", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数校验失败"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "409", description = "角色编码已存在")
    }
)
```

---

## 6. 安全加固

### 6.1 修复 /auth/logout 安全漏洞

**问题**: 当前请求体直接接收 `JwtUserDetails`，暴露 password 字段。

**修复**:
```java
@PostMapping("/api/v1/auth/logout")
public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
    // 从 token 解析 userId，不信任客户端传入
    String token = authorization.replace("Bearer ", "");
    Long userId = jwtUtil.getUserIdFromToken(token);
    
    // 1. 删除 Redis 中的 refresh token
    redisTemplate.delete("refresh_token:" + userId);
    
    // 2. 将 access token 加入黑名单（TTL = token 剩余有效期）
    long remainingTime = jwtUtil.getRemainingTime(token);
    redisTemplate.opsForValue().set(
        "token_blacklist:" + token, 
        "1", 
        remainingTime, 
        TimeUnit.MILLISECONDS
    );
    
    return Result.success(null);
}
```

### 6.2 JWT Token 内容精简

```java
public String generateAccessToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", ((JwtUserDetails) userDetails).getUserId());
    claims.put("authorities", userDetails.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toList()));
    // ✅ 只存必要信息
    // ❌ 不存 password、accountNonLocked 等内部字段
    
    return Jwts.builder()
        .setClaims(claims)
        .setSubject(userDetails.getUsername())
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + accessTokenValidity))
        .signWith(SignatureAlgorithm.HS512, secret)
        .compact();
}
```

### 6.3 Refresh Token 轮换机制

```java
@PostMapping("/api/v1/auth/refresh")
public Result<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
    String oldRefreshToken = request.getRefreshToken();
    
    // 1. 验证 refresh token
    if (!jwtUtil.validateToken(oldRefreshToken)) {
        throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
    }
    
    Long userId = jwtUtil.getUserIdFromToken(oldRefreshToken);
    String username = jwtUtil.getUsernameFromToken(oldRefreshToken);
    
    // 2. 检查 Redis 中是否存在（防止重放）
    String storedToken = redisTemplate.opsForValue().get("refresh_token:" + userId);
    if (!oldRefreshToken.equals(storedToken)) {
        throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID, "Token已失效或被替换");
    }
    
    // 3. 立即删除旧 token（Token Rotation）
    redisTemplate.delete("refresh_token:" + userId);
    
    // 4. 生成新的 token 对
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
    String newAccessToken = jwtUtil.generateAccessToken(userDetails);
    String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);
    
    // 5. 存储新 refresh token
    redisTemplate.opsForValue().set(
        "refresh_token:" + userId,
        newRefreshToken,
        30, TimeUnit.DAYS
    );
    
    return Result.success(new LoginResponse(newAccessToken, newRefreshToken, "Bearer"));
}
```

### 6.4 登录限流与锁定

```java
@PostMapping("/api/v1/auth/login")
public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    String username = request.getUsername();
    
    // 1. 检查账户是否被锁定
    String lockKey = "login_lock:" + username;
    if (redisTemplate.hasKey(lockKey)) {
        throw new BusinessException(ErrorCode.LOCKED, "账户已被锁定15分钟");
    }
    
    // 2. 尝试认证
    try {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, request.getPassword())
        );
        
        // 3. 认证成功，清除失败次数
        redisTemplate.delete("login_failed:" + username);
        
        // 4. 生成 token
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);
        
        // 5. 存储 refresh token
        Long userId = ((JwtUserDetails) userDetails).getUserId();
        redisTemplate.opsForValue().set(
            "refresh_token:" + userId,
            refreshToken,
            30, TimeUnit.DAYS
        );
        
        return Result.success(new LoginResponse(accessToken, refreshToken, "Bearer"));
        
    } catch (BadCredentialsException e) {
        // 6. 认证失败，记录失败次数
        String failKey = "login_failed:" + username;
        Integer failCount = (Integer) redisTemplate.opsForValue().get(failKey);
        failCount = (failCount == null) ? 1 : failCount + 1;
        
        if (failCount >= 5) {
            // 7. 失败5次，锁定15分钟
            redisTemplate.opsForValue().set(lockKey, "1", 15, TimeUnit.MINUTES);
            redisTemplate.delete(failKey);
            throw new BusinessException(ErrorCode.LOCKED, "登录失败次数过多，账户已锁定15分钟");
        } else {
            // 8. 记录失败次数（5分钟过期）
            redisTemplate.opsForValue().set(failKey, failCount, 5, TimeUnit.MINUTES);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, 
                String.format("用户名或密码错误，剩余尝试次数：%d", 5 - failCount));
        }
    }
}
```

### 6.5 注册接口保护

```java
@Data
public class RegisterRequest {
    @NotBlank @Size(min = 3, max = 20) @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    private String username;
    
    @NotBlank @Size(min = 6, max = 64)
    private String password;
    
    @Email @NotBlank
    private String email;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$")
    private String phone;
}

@PostMapping("/api/v1/auth/register")
public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
    // 1. 检查保留用户名
    if (isReservedUsername(request.getUsername())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "该用户名为系统保留");
    }
    
    // 2. 检查用户名是否已存在
    if (userService.existsByUsername(request.getUsername())) {
        throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "用户名已存在");
    }
    
    // 3. 检查邮箱是否已存在
    if (userService.existsByEmail(request.getEmail())) {
        throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "邮箱已被注册");
    }
    
    // 4. 创建用户（默认分配 ROLE_USER）
    userService.register(request);
    
    return Result.success(null);
}

private static final Set<String> RESERVED_USERNAMES = 
    Set.of("admin", "root", "system", "administrator", "superuser");
    
private boolean isReservedUsername(String username) {
    return RESERVED_USERNAMES.contains(username.toLowerCase());
}
```

### 6.6 权限注解规范

```java
@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "角色管理")
public class RoleController {
    
    @GetMapping
    @PreAuthorize("hasAuthority('role:list')")
    @Operation(summary = "角色列表", description = "需要权限：role:list")
    public Result<PageResponse<RoleResponse>> list(...) { }
    
    @PostMapping
    @PreAuthorize("hasAuthority('role:create')")
    @Operation(summary = "创建角色", description = "需要权限：role:create")
    public ResponseEntity<Result<RoleResponse>> create(...) { }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:update')")
    @Operation(summary = "更新角色", description = "需要权限：role:update")
    public Result<RoleResponse> update(...) { }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    @Operation(summary = "删除角色", description = "需要权限：role:delete。若角色已分配给用户，将返回409")
    public Result<Void> delete(...) { }
}
```

---

## 7. 数据访问层调整

### 7.1 分页与过滤参数

```java
// 分页请求基类
@Data
public class PageRequest {
    @Min(1)
    private int page = 1;
    
    @Min(1) @Max(100)
    private int size = 20;
    
    private String sort = "id";
    private String order = "asc";
    
    public int getOffset() {
        return (page - 1) * size;
    }
}

// 用户查询过滤器
@Data
public class UserQueryFilter extends PageRequest {
    private String keyword;      // 用户名/邮箱模糊查询
    private Integer status;      // 状态筛选
    private Boolean locked;      // 锁定状态
    private Long roleId;         // 按角色筛选
}

// 角色查询过滤器
@Data
public class RoleQueryFilter extends PageRequest {
    private String keyword;      // 角色名/编码模糊查询
    private Integer level;       // 按等级筛选
    private String dataScope;    // 按数据范围筛选
}

// 菜单查询过滤器
@Data
public class MenuQueryFilter extends PageRequest {
    private String keyword;      // 菜单名/编码模糊查询
    private Integer menuType;    // 按类型筛选
    private Integer visible;     // 按可见性筛选
    private Integer status;      // 按状态筛选
    private Long parentId;       // 按父级筛选
}
```

### 7.2 UserMapper 新增方法

```java
public interface UserMapper {
    // 现有方法
    SysUser findByUsername(String username);
    List<String> findPermissionsByUserId(Long userId);
    int insert(SysUser user);
    int update(SysUser user);
    
    // 新增方法
    List<SysUser> findAll(@Param("filter") UserQueryFilter filter);
    long countAll(@Param("filter") UserQueryFilter filter);
    SysUser findById(Long id);
    int existsByUsername(String username);
    int existsByEmail(String email);
    int updatePassword(@Param("userId") Long userId, @Param("password") String password);
    int updateLockStatus(@Param("userId") Long userId, @Param("locked") boolean locked);
    List<SysUser> findByRoleId(Long roleId);
    int deleteById(Long id);  // 软删除
}
```

### 7.3 RoleMapper 新增方法

```java
public interface RoleMapper {
    // 现有方法
    SysRole findById(Long id);
    SysRole findByRoleCode(String roleCode);
    List<SysRole> findAll();
    List<SysRole> findRolesByUserId(Long userId);
    int insert(SysRole role);
    int update(SysRole role);
    int deleteById(Long id);
    
    // 新增方法
    List<SysRole> findAllPaged(@Param("filter") RoleQueryFilter filter);
    long countAll(@Param("filter") RoleQueryFilter filter);
    int existsByRoleCode(String roleCode);
    int countUsersByRoleId(Long roleId);
}
```

### 7.4 MenuMapper 新增方法

```java
public interface MenuMapper {
    // 现有方法
    SysMenu findById(Long id);
    List<SysMenu> findAll();
    List<SysMenu> findMenusByRoleId(Long roleId);
    List<SysMenu> findMenusByUserId(Long userId);
    int insert(SysMenu menu);
    int update(SysMenu menu);
    int deleteById(Long id);
    
    // 新增方法
    List<SysMenu> findAllPaged(@Param("filter") MenuQueryFilter filter);
    long countAll(@Param("filter") MenuQueryFilter filter);
    List<SysMenu> findTreeByUserId(Long userId);
    int existsByMenuCode(String menuCode);
    int countRolesByMenuId(Long menuId);
}
```

### 7.5 UserRoleMapper 新增方法

```java
public interface UserRoleMapper {
    // 现有方法
    int insert(SysUserRole userRole);
    int deleteByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);
    int deleteByUserId(Long userId);
    
    // 新增方法
    int insertBatch(@Param("list") List<SysUserRole> userRoles);
    int existsByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);
    List<Long> findRoleIdsByUserId(Long userId);
    int countByRoleId(Long roleId);
}
```

### 7.6 RoleMenuMapper 新增方法

```java
public interface RoleMenuMapper {
    // 现有方法
    int insert(SysRoleMenu roleMenu);
    int deleteByRoleIdAndMenuId(@Param("roleId") Long roleId, @Param("menuId") Long menuId);
    int deleteByRoleId(Long roleId);
    
    // 新增方法
    int insertBatch(@Param("list") List<SysRoleMenu> roleMenus);
    int existsByRoleIdAndMenuId(@Param("roleId") Long roleId, @Param("menuId") Long menuId);
    List<Long> findMenuIdsByRoleId(Long roleId);
    int countByMenuId(Long menuId);
}
```

### 7.7 XML 示例（分页查询）

```xml
<!-- RoleMapper.xml -->
<select id="findAllPaged" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List"/>
    FROM sys_role
    <where>
        <if test="filter.keyword != null and filter.keyword != ''">
            AND (role_name LIKE CONCAT('%', #{filter.keyword}, '%')
                 OR role_code LIKE CONCAT('%', #{filter.keyword}, '%'))
        </if>
        <if test="filter.level != null">
            AND level = #{filter.level}
        </if>
        <if test="filter.dataScope != null and filter.dataScope != ''">
            AND data_scope = #{filter.dataScope}
        </if>
    </where>
    ORDER BY ${filter.sort} ${filter.order}
    LIMIT #{filter.offset}, #{filter.size}
</select>

<select id="countAll" resultType="long">
    SELECT COUNT(*)
    FROM sys_role
    <where>
        <if test="filter.keyword != null and filter.keyword != ''">
            AND (role_name LIKE CONCAT('%', #{filter.keyword}, '%')
                 OR role_code LIKE CONCAT('%', #{filter.keyword}, '%'))
        </if>
        <if test="filter.level != null">
            AND level = #{filter.level}
        </if>
        <if test="filter.dataScope != null and filter.dataScope != ''">
            AND data_scope = #{filter.dataScope}
        </if>
    </where>
</select>

<select id="existsByRoleCode" resultType="int">
    SELECT COUNT(*) FROM sys_role WHERE role_code = #{roleCode}
</select>

<select id="countUsersByRoleId" resultType="int">
    SELECT COUNT(*) FROM sys_user_role WHERE role_id = #{roleId}
</select>
```

---

## 8. 测试策略

### 8.1 测试分层

```
单元测试（Service 层）
├── RoleServiceTest
├── MenuServiceTest  
├── UserServiceTest
└── AuthServiceTest

集成测试（Controller 层）
├── RoleControllerTest
├── MenuControllerTest
├── UserControllerTest
└── AuthControllerTest

数据访问测试（Mapper 层）
├── RoleMapperTest
├── MenuMapperTest
├── UserMapperTest
└── 关联表 Mapper 测试
```

### 8.2 测试覆盖率目标

- Controller 层：≥ 90%（覆盖所有 HTTP 状态码路径）
- Service 层：≥ 85%（覆盖核心业务逻辑和异常场景）
- Mapper 层：≥ 80%（覆盖复杂 SQL 和分页查询）

### 8.3 Controller 集成测试模板

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoleControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private String adminToken;
    
    @BeforeEach
    void setUp() throws Exception {
        // 登录获取 token
        LoginRequest loginRequest = new LoginRequest("admin", "admin123");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();
        
        Result<LoginResponse> response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            new TypeReference<Result<LoginResponse>>() {}
        );
        
        adminToken = response.getData().getAccessToken();
    }
    
    @Test
    void shouldCreateRole() throws Exception {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setRoleCode("ROLE_TEST");
        request.setRoleName("测试角色");
        request.setLevel(5);
        request.setDataScope(DataScopeEnum.DEPT);
        
        mockMvc.perform(post("/api/v1/roles")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.roleCode").value("ROLE_TEST"))
            .andExpect(header().exists("Location"));
    }
    
    @Test
    void shouldReturn404WhenRoleNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/roles/99999")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(40402));
    }
    
    @Test
    void shouldReturn409WhenDeleteRoleInUse() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/1")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(40902));
    }
}
```

### 8.4 Service 单元测试模板

```java
@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {
    
    @Mock
    private RoleMapper roleMapper;
    
    @Mock
    private UserRoleMapper userRoleMapper;
    
    @InjectMocks
    private RoleServiceImpl roleService;
    
    @Test
    void shouldThrowExceptionWhenRoleCodeExists() {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setRoleCode("ROLE_ADMIN");
        
        when(roleMapper.existsByRoleCode("ROLE_ADMIN")).thenReturn(1);
        
        assertThrows(BusinessException.class, () -> {
            roleService.create(request);
        });
        
        verify(roleMapper, never()).insert(any());
    }
    
    @Test
    void shouldThrowExceptionWhenDeleteRoleInUse() {
        when(userRoleMapper.countByRoleId(1L)).thenReturn(5);
        
        BusinessException ex = assertThrows(BusinessException.class, () -> {
            roleService.delete(1L, false);
        });
        
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_IN_USE);
        verify(roleMapper, never()).deleteById(any());
    }
}
```

### 8.5 测试数据管理

SQL 文件位于 `docs/sql/V003__test_data.sql`：

```sql
-- 测试数据（密码：admin123）
INSERT INTO sys_user (id, username, password, email, status, locked) VALUES
(1, 'admin', '$2a$10$SG7HkhWRQvmDzCG3S.aYsOxxezisRSSm.jRm4uXszZMdpf0g3JuMO', 'admin@test.com', 1, 0),
(2, 'user', '$2a$10$SG7HkhWRQvmDzCG3S.aYsOxxezisRSSm.jRm4uXszZMdpf0g3JuMO', 'user@test.com', 1, 0);

INSERT INTO sys_role (id, role_code, role_name, level, data_scope) VALUES
(1, 'ROLE_ADMIN', '管理员', 1, 'ALL'),
(2, 'ROLE_USER', '普通用户', 5, 'SELF');

INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, perms, status) VALUES
(1, 0, '系统管理', 'system', 1, NULL, 1),
(2, 1, '角色管理', 'role', 2, 'role:list', 1),
(3, 2, '新增角色', 'role-create', 3, 'role:create', 1);

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1), (2, 2);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1), (1, 2), (1, 3);
```

---

## 9. 实施路线图

### 阶段 1：基础设施（2-3 天）

**目标**: 建立错误处理、DTO 体系、枚举定义

1. 创建错误码枚举 `ErrorCode`
2. 实现全局异常处理器 `GlobalExceptionHandler`
3. 增强 `Result` 类（添加 traceId、timestamp）
4. 创建自定义异常类（BusinessException、ResourceNotFoundException）
5. 定义枚举（DataScopeEnum、MenuTypeEnum）
6. 创建所有 DTO 类（Request/Response/PageResponse）
7. 创建分页与过滤参数类

### 阶段 2：认证模块优化（2 天）

**目标**: 修复安全漏洞，实现 Token 轮换

1. 修复 `/auth/logout` 安全问题（从 token 获取用户）
2. 精简 JWT Token 内容（移除敏感字段）
3. 实现 Refresh Token 轮换机制
4. 实现登录限流与账户锁定（Redis）
5. 增强注册接口（保留用户名检查、邮箱唯一性）
6. 更新 `AuthController` 路径为 `/api/v1/auth/**`
7. 补充 OpenAPI 文档和错误响应声明

### 阶段 3：用户管理模块（3 天）

**目标**: 补齐完整 CRUD 和关联管理

1. 扩展 UserMapper（新增方法 + XML）
2. 扩展 UserRoleMapper（批量插入、反查等）
3. 实现 UserService 完整业务逻辑
4. 重构 UserController：
   - GET `/api/v1/users` 分页列表
   - GET `/api/v1/users/{id}` 详情
   - POST `/api/v1/users` 创建
   - PUT `/api/v1/users/{id}` 更新
   - DELETE `/api/v1/users/{id}` 删除
   - PUT `/api/v1/users/{id}/roles` 全量替换角色
   - POST `/api/v1/users/{id}/roles` 追加角色
   - DELETE `/api/v1/users/{id}/roles/{roleId}` 解绑角色
   - POST `/api/v1/users/{id}/lock` 锁定
   - POST `/api/v1/users/{id}/unlock` 解锁（迁移自 AuthController）
   - POST `/api/v1/users/{id}/password:reset` 管理员重置密码
   - GET `/api/v1/users/me` 当前用户信息
   - PUT `/api/v1/users/me/password` 修改密码
5. 编写单元测试和集成测试

### 阶段 4：角色管理模块（2 天）

**目标**: 优化 CRUD，修正 HTTP 语义

1. 扩展 RoleMapper（分页、过滤、统计等）
2. 扩展 RoleMenuMapper（批量操作）
3. 实现 RoleService 业务逻辑（删除前检查引用）
4. 重构 RoleController：
   - GET `/api/v1/roles` 分页列表
   - GET `/api/v1/roles/{id}` 详情
   - POST `/api/v1/roles` 创建（返回 201）
   - PUT `/api/v1/roles/{id}` 更新
   - DELETE `/api/v1/roles/{id}` 删除（支持 force）
   - GET `/api/v1/roles/{id}/menus` 查询角色菜单
   - PUT `/api/v1/roles/{id}/menus` 全量替换菜单
   - DELETE `/api/v1/roles/{id}/menus/{menuId}` 解绑菜单
   - GET `/api/v1/roles/{id}/users` 反查用户
5. 编写测试

### 阶段 5：菜单管理模块（2 天）

**目标**: 支持树形结构和分页查询

1. 扩展 MenuMapper（分页、树形查询、统计等）
2. 实现 MenuService 业务逻辑
3. 重构 MenuController：
   - GET `/api/v1/menus` 分页列表
   - GET `/api/v1/menus/tree` 树形结构
   - GET `/api/v1/menus/{id}` 详情
   - POST `/api/v1/menus` 创建
   - PUT `/api/v1/menus/{id}` 更新
   - DELETE `/api/v1/menus/{id}` 删除
   - GET `/api/v1/menus/{id}/roles` 反查角色
4. 实现树形结构构建算法
5. 编写测试

### 阶段 6：文档与体系化（1-2 天）

**目标**: 完善 OpenAPI 文档，生成测试数据

1. 为所有接口补充完整的 `@Operation` 注解
2. 添加字段级 `description` 和 `example`
3. 声明所有错误响应（400/401/403/404/409）
4. 生成测试数据 SQL（`docs/sql/V003__test_data.sql`）
5. 完善 README 和 API 文档
6. 代码审查与重构

---

## 10. 验收标准

### 10.1 功能完整性

- [ ] 认证模块：登录、登出、刷新、注册全部正常
- [ ] 用户管理：完整 CRUD + 角色分配 + 锁定/解锁 + 密码管理
- [ ] 角色管理：完整 CRUD + 菜单分配 + 删除保护
- [ ] 菜单管理：完整 CRUD + 树形查询 + 删除保护
- [ ] 分页查询：所有列表接口支持分页和过滤
- [ ] 关联管理：PUT 全量替换、POST 追加、DELETE 解绑

### 10.2 安全性

- [ ] JwtUserDetails 不再出现在 API 中
- [ ] Token 黑名单机制生效
- [ ] Refresh Token 轮换正常
- [ ] 登录失败 5 次触发锁定
- [ ] 所有接口正确校验权限

### 10.3 规范性

- [ ] 所有路径统一到 `/api/v1/`
- [ ] HTTP 方法语义正确（PUT 替换、POST 追加、DELETE 解绑）
- [ ] 创建接口返回 201 + Location
- [ ] 错误响应包含正确的状态码和错误码
- [ ] OpenAPI 文档完整（description + example + 错误响应）

### 10.4 测试覆盖

- [ ] Controller 层测试覆盖率 ≥ 90%
- [ ] Service 层测试覆盖率 ≥ 85%
- [ ] Mapper 层测试覆盖率 ≥ 80%
- [ ] 核心流程有端到端测试

---

## 11. 风险与注意事项

### 11.1 技术风险

- **MyBatis 动态排序**: `ORDER BY ${filter.sort} ${filter.order}` 使用 `${}` 存在 SQL 注入风险，需在 Service 层白名单校验
- **Redis 依赖**: 登录限流、Token 黑名单依赖 Redis，需确保 Redis 可用性

### 11.2 数据迁移

- 无数据迁移问题（教学项目，重新初始化数据库即可）
- 建议保留旧数据备份：`mysqldump jwt_java_eight > backup_before_optimization.sql`

### 11.3 性能考虑

- 分页查询默认上限 100 条/页，防止大查询
- 菜单树形结构查询无分页，若菜单超过 500 个需优化为懒加载

---

## 12. 附录

### 12.1 保留用户名列表

```java
private static final Set<String> RESERVED_USERNAMES = Set.of(
    "admin", "root", "system", "administrator", "superuser",
    "support", "help", "info", "webmaster", "postmaster"
);
```

### 12.2 数据范围枚举说明

| 枚举值 | 说明 | 应用场景 |
|-------|------|---------|
| ALL | 全部数据权限 | 超级管理员 |
| DEPT | 本部门数据权限 | 部门经理 |
| DEPT_AND_SUB | 本部门及下级部门 | 分管领导 |
| SELF | 仅本人数据 | 普通员工 |
| CUSTOM | 自定义数据权限 | 特殊角色（需扩展表） |

### 12.3 菜单类型说明

| 类型 | code | 说明 | perms 字段 |
|-----|------|------|-----------|
| DIR | 1 | 目录 | 可为空 |
| MENU | 2 | 菜单 | 通常为 `xxx:list` |
| BUTTON | 3 | 按钮 | 必填，如 `role:create` |

---

**设计审查**: 待用户确认后进入实施阶段
