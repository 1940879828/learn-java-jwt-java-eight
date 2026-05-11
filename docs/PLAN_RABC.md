# RBAC 升级计划

## 目录

1. [现状总结](#1-现状总结)
2. [问题诊断](#2-问题诊断)
3. [升级目标](#3-升级目标)
4. [详细实施步骤](#4-详细实施步骤)
5. [数据迁移策略](#5-数据迁移策略)
6. [测试策略](#6-测试策略)
7. [资源与时间线估算](#7-资源与时间线估算)
8. [风险评估](#8-风险评估)

---

## 1. 现状总结

### 1.1 当前 RBAC 实现程度评估
**综合评分：68/100**

| 维度 | 评分 | 说明 |
|------|------|------|
| 架构设计 | 75 | 双层安全配置，但使用已废弃的 `WebSecurityConfigurerAdapter` |
| 数据模型 | 65 | 存在用户-角色关联，但权限表结构不完整 |
| 权限校验逻辑 | 70 | 支持方法级权限，但URL级权限控制较粗 |
| 业务集成度 | 60 | 部分控制器使用权限注解，但覆盖不全 |
| 安全性 | 65 | 存在硬编码密钥和权限绕过风险 |

### 1.2 技术架构优缺点分析

**优点：**
- 双层安全配置分离内部API和用户API
- 无状态会话管理（SessionCreationPolicy.STATELESS）
- JWT Token中携带权限信息
- 支持方法级权限注解（`@PreAuthorize`）

**缺点：**
- 使用已废弃的 `WebSecurityConfigurerAdapter`（Spring Security 5.7+）
- JWT密钥硬编码且强度不足
- 部分端点过于开放（如 `/actuator/**`）
- 缺乏统一的权限管理入口

### 1.3 数据模型完整性评估

**当前表结构：**
- `t_user`：用户基本信息表
- `t_role`：角色表
- `t_user_role`：用户-角色关联表
- `t_permission`：权限表（结构不完整）

**问题：**
- 权限表缺少资源类型、操作类型等字段
- 缺乏角色-权限关联表
- 索引设计不合理，查询性能差

### 1.4 权限校验逻辑覆盖度

**URL级权限控制：**
```java
.antMatchers("/api/**", "/auth/**").authenticated();
```
- 仅验证认证状态，无细粒度角色控制

**方法级权限控制：**
```java
@PreAuthorize("hasAuthority('user:list')")
@PreAuthorize("hasAuthority('user:save')")
```
- 部分控制器使用，但覆盖不全

**数据级权限控制：**
- 未实现行级数据过滤

### 1.5 业务集成度分析

**控制器层：**
- 部分控制器使用 `@PreAuthorize` 注解
- 缺乏统一的权限校验标准

**服务层：**
- 无服务层权限校验逻辑
- 权限校验完全依赖控制器层

**数据访问层：**
- 无数据级权限过滤

---

## 2. 问题诊断

### 2.1 严重问题（P0）

| 问题 | 风险等级 | 描述 |
|------|----------|------|
| JWT密钥硬编码 | 高 | 密钥 `"abcdefgh"` 硬编码在代码中，易被破解 |
| 权限绕过风险 | 高 | 部分端点（如 `/actuator/**`）完全开放，无认证要求 |
| 数据泄露风险 | 高 | 缺乏数据级权限控制，用户可能访问他人数据 |
| 缺乏审计日志 | 中 | 权限变更操作无审计记录，难以追踪 |

### 2.2 中等问题（P1）

| 问题 | 风险等级 | 描述 |
|------|----------|------|
| 性能瓶颈 | 中 | 每次请求都解析JWT，无权限缓存机制 |
| 扩展困难 | 中 | 权限模型设计不合理，难以支持新业务 |
| 维护成本高 | 中 | 权限逻辑分散，缺乏统一管理入口 |
| 配置复杂 | 中 | 权限字符串硬编码，维护困难 |

### 2.3 轻微问题（P2）

| 问题 | 风险等级 | 描述 |
|------|----------|------|
| 代码规范 | 低 | 权限注解使用不统一 |
| 文档缺失 | 低 | 缺乏权限设计文档和API文档 |
| 测试覆盖不足 | 低 | 权限校验逻辑缺乏单元测试 |

### 2.4 问题优先级排序

1. **P0（立即修复）**：JWT密钥安全、权限绕过、数据泄露
2. **P1（1周内）**：基础RBAC模型建立、关键接口权限控制
3. **P2（2-4周）**：权限管理界面、权限缓存优化
4. **P3（1-2月）**：高级特性、性能优化、文档完善

---

## 3. 升级目标

### 3.1 短期目标（1-2周）

- [ ] 修复JWT密钥安全问题
- [ ] 建立基础RBAC数据模型
- [ ] 实现URL级细粒度权限控制
- [ ] 统一权限注解使用规范

### 3.2 中期目标（1-2月）

- [ ] 完善权限管理体系
- [ ] 实现权限缓存机制
- [ ] 开发权限管理界面
- [ ] 实现数据级权限控制

### 3.3 长期目标（3-6月）

- [ ] 实现RBAC1（角色继承）
- [ ] 实现RBAC2（约束机制）
- [ ] 支持多租户权限隔离
- [ ] 实现权限审计和监控

---

## 4. 详细实施步骤

### 阶段一：数据模型建设（1-2周）

#### 4.1 数据库表设计

**用户表（sys_user）**
```sql
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

**角色表（sys_role）**
```sql
CREATE TABLE `sys_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';
```

**权限表（sys_permission）**
```sql
CREATE TABLE `sys_permission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `parent_id` bigint(20) DEFAULT 0 COMMENT '父权限ID',
  `permission_name` varchar(50) NOT NULL COMMENT '权限名称',
  `permission_code` varchar(100) NOT NULL COMMENT '权限编码',
  `resource_type` varchar(20) DEFAULT NULL COMMENT '资源类型：menu/button/api',
  `resource_url` varchar(200) DEFAULT NULL COMMENT '资源URL',
  `permission_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '权限类型：1-菜单，2-按钮，3-API',
  `sort_order` int(11) DEFAULT 0 COMMENT '排序',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';
```

**用户-角色关联表（sys_user_role）**
```sql
CREATE TABLE `sys_user_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';
```

**角色-权限关联表（sys_role_permission）**
```sql
CREATE TABLE `sys_role_permission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `permission_id` bigint(20) NOT NULL COMMENT '权限ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联表';
```

#### 4.2 实体类创建

**用户实体**
```java
@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String email;
    private String phone;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

**角色实体**
```java
@Data
@TableName("sys_role")
public class SysRole {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roleName;
    private String roleCode;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

**权限实体**
```java
@Data
@TableName("sys_permission")
public class SysPermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String permissionName;
    private String permissionCode;
    private String resourceType;
    private String resourceUrl;
    private Integer permissionType;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

#### 4.3 MyBatis Mapper接口

```java
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    @Select("SELECT u.* FROM sys_user u " +
            "INNER JOIN sys_user_role ur ON u.id = ur.user_id " +
            "WHERE ur.role_id = #{roleId}")
    List<SysUser> selectUsersByRoleId(@Param("roleId") Long roleId);
}

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
    @Select("SELECT r.* FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);
}

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {
    @Select("SELECT p.* FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId}")
    List<SysPermission> selectPermissionsByRoleId(@Param("roleId") Long roleId);
}
```

#### 4.4 数据初始化脚本

```sql
-- 初始化管理员角色
INSERT INTO `sys_role` (`role_name`, `role_code`, `description`) VALUES
('超级管理员', 'ROLE_ADMIN', '系统超级管理员'),
('普通用户', 'ROLE_USER', '普通用户角色');

-- 初始化基础权限
INSERT INTO `sys_permission` (`permission_name`, `permission_code`, `resource_type`, `permission_type`) VALUES
('用户管理', 'user:manage', 'menu', 1),
('用户查看', 'user:list', 'button', 2),
('用户新增', 'user:save', 'button', 2),
('用户编辑', 'user:update', 'button', 2),
('用户删除', 'user:delete', 'button', 2);

-- 分配权限给角色
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5),
(2, 1), (2, 2);
```

### 阶段二：权限加载链路改造（1-2周）

#### 4.5 用户详情服务改造

```java
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private SysRoleMapper roleMapper;
    @Autowired
    private SysPermissionMapper permissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 查询用户
        SysUser user = userMapper.selectOne(
            new QueryWrapper<SysUser>().eq("username", username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 2. 查询角色
        List<SysRole> roles = roleMapper.selectRolesByUserId(user.getId());
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // 3. 添加角色权限
        for (SysRole role : roles) {
            authorities.add(new SimpleGrantedAuthority(role.getRoleCode()));
            
            // 4. 查询权限
            List<SysPermission> permissions = permissionMapper.selectPermissionsByRoleId(role.getId());
            for (SysPermission permission : permissions) {
                authorities.add(new SimpleGrantedAuthority(permission.getPermissionCode()));
            }
        }

        return new User(user.getUsername(), user.getPassword(), authorities);
    }
}
```

#### 4.6 JWT Token中权限信息处理

```java
public class JwtTokenUtils {
    // 从配置文件读取密钥
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userDetails.getUsername());
        claims.put("created", new Date());
        
        // 提取权限列表
        List<String> authorities = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());
        claims.put("authorities", authorities);
        
        return Jwts.builder()
            .setClaims(claims)
            .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
    }
}
```

#### 4.7 权限缓存机制实现

```java
@Service
public class PermissionCacheService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String PERMISSION_CACHE_KEY = "user:permission:";
    private static final long CACHE_EXPIRE_TIME = 30; // 分钟

    public Set<String> getUserPermissions(String username) {
        String key = PERMISSION_CACHE_KEY + username;
        Set<String> permissions = (Set<String>) redisTemplate.opsForValue().get(key);
        
        if (permissions == null) {
            // 从数据库加载
            permissions = loadPermissionsFromDB(username);
            // 缓存到Redis
            redisTemplate.opsForValue().set(key, permissions, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        }
        
        return permissions;
    }

    public void clearUserPermissionCache(String username) {
        String key = PERMISSION_CACHE_KEY + username;
        redisTemplate.delete(key);
    }
}
```

### 阶段三：权限校验启用（1-2周）

#### 4.8 启用方法级安全注解

```java
@Configuration
@EnableGlobalMethodSecurity(
    prePostEnabled = true,
    securedEnabled = true,
    jsr250Enabled = true
)
public class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {
    @Override
    protected MethodSecurityExpressionHandler createExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler = 
            new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(new CustomPermissionEvaluator());
        return expressionHandler;
    }
}
```

#### 4.9 URL级权限规则配置

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .exceptionHandling().authenticationEntryPoint(unauthorizedHandler)
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                // 公开接口
                .antMatchers("/auth/**", "/actuator/health").permitAll()
                // 管理员接口
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                // 用户管理接口
                .antMatchers(HttpMethod.GET, "/api/users/**").hasAuthority("user:list")
                .antMatchers(HttpMethod.POST, "/api/users/**").hasAuthority("user:save")
                .antMatchers(HttpMethod.PUT, "/api/users/**").hasAuthority("user:update")
                .antMatchers(HttpMethod.DELETE, "/api/users/**").hasAuthority("user:delete")
                // 其他接口需要认证
                .anyRequest().authenticated();
        
        // 添加JWT过滤器
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    }
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }
}
```

#### 4.10 自定义权限校验器

```java
@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {
    @Autowired
    private PermissionCacheService permissionCacheService;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        String username = authentication.getName();
        Set<String> userPermissions = permissionCacheService.getUserPermissions(username);
        
        return userPermissions.contains(permission.toString());
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        // 实现基于ID的权限校验
        return hasPermission(authentication, null, permission);
    }
}
```

#### 4.11 权限校验AOP切面

```java
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PermissionAspect {
    @Autowired
    private PermissionCacheService permissionCacheService;

    @Around("@annotation(requiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequiresPermission requiresPermission) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("未认证");
        }

        String username = authentication.getName();
        String requiredPermission = requiresPermission.value();
        
        Set<String> userPermissions = permissionCacheService.getUserPermissions(username);
        if (!userPermissions.contains(requiredPermission)) {
            throw new AccessDeniedException("权限不足");
        }

        return joinPoint.proceed();
    }
}

// 自定义权限注解
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    String value();
}
```

### 阶段四：管理接口建设（2-3周）

#### 4.12 角色管理API

```java
@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {
    @Autowired
    private SysRoleService roleService;

    @GetMapping
    public Result<List<SysRole>> listRoles() {
        return Result.success(roleService.list());
    }

    @PostMapping
    public Result<SysRole> createRole(@RequestBody @Valid SysRole role) {
        return Result.success(roleService.saveRole(role));
    }

    @PutMapping("/{id}")
    public Result<SysRole> updateRole(@PathVariable Long id, @RequestBody @Valid SysRole role) {
        role.setId(id);
        return Result.success(roleService.updateRole(role));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    @PostMapping("/{roleId}/permissions")
    public Result<Void> assignPermissions(@PathVariable Long roleId, @RequestBody List<Long> permissionIds) {
        roleService.assignPermissions(roleId, permissionIds);
        return Result.success();
    }
}
```

#### 4.13 权限管理API

```java
@RestController
@RequestMapping("/api/permissions")
@PreAuthorize("hasRole('ADMIN')")
public class PermissionController {
    @Autowired
    private SysPermissionService permissionService;

    @GetMapping("/tree")
    public Result<List<PermissionTree>> getPermissionTree() {
        return Result.success(permissionService.getPermissionTree());
    }

    @PostMapping
    public Result<SysPermission> createPermission(@RequestBody @Valid SysPermission permission) {
        return Result.success(permissionService.savePermission(permission));
    }

    @PutMapping("/{id}")
    public Result<SysPermission> updatePermission(@PathVariable Long id, @RequestBody @Valid SysPermission permission) {
        permission.setId(id);
        return Result.success(permissionService.updatePermission(permission));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return Result.success();
    }
}
```

#### 4.14 用户角色分配API

```java
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    @Autowired
    private SysUserService userService;

    @PostMapping("/{userId}/roles")
    public Result<Void> assignRoles(@PathVariable Long userId, @RequestBody List<Long> roleIds) {
        userService.assignRoles(userId, roleIds);
        return Result.success();
    }

    @GetMapping("/{userId}/roles")
    public Result<List<SysRole>> getUserRoles(@PathVariable Long userId) {
        return Result.success(userService.getUserRoles(userId));
    }

    @GetMapping("/{userId}/permissions")
    public Result<List<SysPermission>> getUserPermissions(@PathVariable Long userId) {
        return Result.success(userService.getUserPermissions(userId));
    }
}
```

### 阶段五：高级特性实现（3-4周）

#### 4.15 角色继承机制

```java
@Entity
@Table(name = "sys_role")
public class SysRole {
    // ... 其他字段
    
    @Column(name = "parent_id")
    private Long parentId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private SysRole parentRole;
    
    @OneToMany(mappedBy = "parentRole", fetch = FetchType.LAZY)
    private List<SysRole> childRoles;
}

@Service
public class RoleHierarchyService {
    public Set<String> getEffectivePermissions(Long roleId) {
        Set<String> permissions = new HashSet<>();
        SysRole role = roleMapper.selectById(roleId);
        
        // 获取当前角色权限
        List<SysPermission> rolePermissions = permissionMapper.selectPermissionsByRoleId(roleId);
        permissions.addAll(rolePermissions.stream()
            .map(SysPermission::getPermissionCode)
            .collect(Collectors.toList()));
        
        // 递归获取父角色权限
        if (role.getParentId() != null) {
            permissions.addAll(getEffectivePermissions(role.getParentId()));
        }
        
        return permissions;
    }
}
```

#### 4.16 数据权限控制

```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {
    String tableAlias() default "";
    String departmentColumn() default "department_id";
    String userColumn() default "create_by";
}

@Aspect
@Component
public class DataScopeAspect {
    @Before("@annotation(dataScope)")
    public void doBefore(JoinPoint point, DataScope dataScope) {
        // 获取当前用户
        SysUser currentUser = SecurityUtils.getCurrentUser();
        
        // 构建数据权限SQL
        String sqlFilter = buildDataScopeSql(currentUser, dataScope);
        
        // 设置到ThreadLocal，供MyBatis拦截器使用
        DataScopeContextHolder.setDataScope(sqlFilter);
    }
    
    @After("@annotation(dataScope)")
    public void doAfter(JoinPoint point, DataScope dataScope) {
        DataScopeContextHolder.clear();
    }
}

// MyBatis拦截器
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class DataScopeInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        String dataScope = DataScopeContextHolder.getDataScope();
        if (StringUtils.isNotBlank(dataScope)) {
            // 修改SQL，添加数据权限条件
            StatementHandler handler = (StatementHandler) invocation.getTarget();
            MetaObject metaObject = SystemMetaObject.forObject(handler);
            String originalSql = (String) metaObject.getValue("delegate.boundSql.sql");
            String newSql = originalSql + " AND " + dataScope;
            metaObject.setValue("delegate.boundSql.sql", newSql);
        }
        return invocation.proceed();
    }
}
```

#### 4.17 审计日志增强

```java
@Aspect
@Component
public class AuditLogAspect {
    @Autowired
    private AuditLogService auditLogService;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;
        
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            // 记录审计日志
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(SecurityUtils.getCurrentUserId());
            auditLog.setUsername(SecurityUtils.getCurrentUsername());
            auditLog.setOperation(auditable.operation());
            auditLog.setMethod(joinPoint.getSignature().toShortString());
            auditLog.setParams(JSON.toJSONString(joinPoint.getArgs()));
            auditLog.setResult(exception == null ? "成功" : "失败");
            auditLog.setError(exception != null ? exception.getMessage() : null);
            auditLog.setCostTime(System.currentTimeMillis() - startTime);
            auditLog.setIp(getClientIp());
            auditLog.setCreateTime(new Date());
            
            auditLogService.save(auditLog);
        }
    }
}

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String operation();
}
```

---

## 5. 数据迁移策略

### 5.1 迁移前数据备份

```bash
# 备份现有数据
mysqldump -u root -p riveroll_workbench > backup_$(date +%Y%m%d_%H%M%S).sql

# 备份用户表
mysqldump -u root -p riveroll_workbench t_user > user_backup_$(date +%Y%m%d_%H%M%S).sql

# 备份角色表
mysqldump -u root -p riveroll_workbench t_role > role_backup_$(date +%Y%m%d_%H%M%S).sql
```

### 5.2 增量迁移脚本

```sql
-- 1. 创建新表结构
source rbac_schema.sql;

-- 2. 迁移用户数据
INSERT INTO sys_user (id, username, password, real_name, email, phone, status, create_time)
SELECT id, username, password, real_name, email, phone, status, create_time FROM t_user;

-- 3. 迁移角色数据
INSERT INTO sys_role (id, role_name, role_code, description, status, create_time)
SELECT id, role_name, role_code, description, status, create_time FROM t_role;

-- 4. 迁移用户-角色关联
INSERT INTO sys_user_role (user_id, role_id, create_time)
SELECT user_id, role_id, create_time FROM t_user_role;

-- 5. 初始化权限数据
source init_permissions.sql;

-- 6. 分配默认权限
source assign_default_permissions.sql;
```

### 5.3 数据验证和修复

```sql
-- 验证用户数据完整性
SELECT COUNT(*) as total_users FROM sys_user;
SELECT COUNT(*) as migrated_users FROM t_user;

-- 验证角色数据完整性
SELECT COUNT(*) as total_roles FROM sys_role;
SELECT COUNT(*) as migrated_roles FROM t_role;

-- 验证关联数据完整性
SELECT COUNT(*) as total_user_roles FROM sys_user_role;
SELECT COUNT(*) as migrated_user_roles FROM t_user_role;

-- 检查孤立数据
SELECT ur.user_id FROM sys_user_role ur 
LEFT JOIN sys_user u ON ur.user_id = u.id 
WHERE u.id IS NULL;

SELECT ur.role_id FROM sys_user_role ur 
LEFT JOIN sys_role r ON ur.role_id = r.id 
WHERE r.id IS NULL;
```

### 5.4 回滚方案

```sql
-- 回滚脚本
-- 1. 删除新表
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

-- 2. 恢复备份
mysql -u root -p riveroll_workbench < backup_20260511_220000.sql;

-- 3. 验证恢复结果
SELECT COUNT(*) FROM t_user;
SELECT COUNT(*) FROM t_role;
```

---

## 6. 测试策略

### 6.1 单元测试

```java
@SpringBootTest
@RunWith(SpringRunner.class)
public class PermissionServiceTest {
    @Autowired
    private PermissionService permissionService;
    
    @MockBean
    private SysPermissionMapper permissionMapper;
    
    @Test
    public void testGetUserPermissions() {
        // 准备测试数据
        Long userId = 1L;
        List<SysPermission> mockPermissions = Arrays.asList(
            new SysPermission(1L, "user:list", "用户查看"),
            new SysPermission(2L, "user:save", "用户新增")
        );
        
        when(permissionMapper.selectPermissionsByUserId(userId)).thenReturn(mockPermissions);
        
        // 执行测试
        Set<String> permissions = permissionService.getUserPermissions(userId);
        
        // 验证结果
        assertThat(permissions).containsExactlyInAnyOrder("user:list", "user:save");
    }
    
    @Test(expected = AccessDeniedException.class)
    public void testPermissionDenied() {
        // 测试权限不足的情况
        permissionService.checkPermission("user:delete", "ROLE_USER");
    }
}
```

### 6.2 集成测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class PermissionIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    public void testUserListWithPermission() {
        // 使用有权限的用户
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAdminToken());
        
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/users",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    
    @Test
    public void testUserListWithoutPermission() {
        // 使用无权限的用户
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getUserToken());
        
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/users",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
```

### 6.3 安全测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class SecurityTest {
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    public void testUnauthorizedAccess() {
        // 测试未认证访问
        ResponseEntity<String> response = restTemplate.getForEntity("/api/users", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    @Test
    public void testExpiredToken() {
        // 测试过期Token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getExpiredToken());
        
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/users",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    @Test
    public void testRoleEscalation() {
        // 测试角色提升攻击
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getUserToken());
        
        // 尝试访问管理员接口
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/admin/users",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
```

### 6.4 性能测试

```java
@SpringBootTest
@RunWith(SpringRunner.class)
public class PermissionPerformanceTest {
    @Autowired
    private PermissionCacheService permissionCacheService;
    
    @Test
    public void testPermissionLoadPerformance() {
        int iterations = 1000;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            permissionCacheService.getUserPermissions("testuser");
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 验证性能：1000次查询应在5秒内完成
        assertThat(duration).isLessThan(5000);
        
        System.out.println("Permission load performance: " + duration + "ms for " + iterations + " iterations");
    }
    
    @Test
    public void testCacheHitRate() {
        // 预热缓存
        permissionCacheService.getUserPermissions("testuser");
        
        // 统计缓存命中率
        int hits = 0;
        int total = 100;
        
        for (int i = 0; i < total; i++) {
            long startTime = System.currentTimeMillis();
            permissionCacheService.getUserPermissions("testuser");
            long duration = System.currentTimeMillis() - startTime;
            
            // 缓存命中应该很快（<10ms）
            if (duration < 10) {
                hits++;
            }
        }
        
        double hitRate = (double) hits / total * 100;
        System.out.println("Cache hit rate: " + hitRate + "%");
        
        // 验证缓存命中率>90%
        assertThat(hitRate).isGreaterThan(90);
    }
}
```

### 6.5 回归测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class RegressionTest {
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    public void testExistingFunctionality() {
        // 测试现有功能是否正常工作
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getValidToken());
        
        // 测试用户列表
        ResponseEntity<String> userListResponse = restTemplate.exchange(
            "/api/users",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );
        assertThat(userListResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // 测试创建用户
        UserCreateRequest request = new UserCreateRequest("newuser", "password", "新用户");
        ResponseEntity<String> createUserResponse = restTemplate.exchange(
            "/api/users",
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            String.class
        );
        assertThat(createUserResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // 测试更新用户
        UserUpdateRequest updateRequest = new UserUpdateRequest(1L, "updateduser", "更新用户");
        ResponseEntity<String> updateUserResponse = restTemplate.exchange(
            "/api/users/1",
            HttpMethod.PUT,
            new HttpEntity<>(updateRequest, headers),
            String.class
        );
        assertThat(updateUserResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

---

## 7. 资源与时间线估算

### 7.1 开发人员需求

| 角色 | 人数 | 职责 |
|------|------|------|
| 后端开发 | 2人 | RBAC核心功能开发、API开发 |
| 前端开发 | 1人 | 权限管理界面开发 |
| DBA | 1人 | 数据库设计、数据迁移、性能优化 |
| 测试工程师 | 1人 | 测试用例设计、执行测试 |

### 7.2 测试人员需求

| 测试阶段 | 人员 | 时间 |
|----------|------|------|
| 单元测试 | 后端开发 | 持续进行 |
| 集成测试 | 测试工程师 | 1周 |
| 安全测试 | 安全专家 | 3天 |
| 性能测试 | 测试工程师 | 2天 |
| 回归测试 | 测试工程师 | 3天 |

### 7.3 环境资源需求

| 环境 | 配置 | 用途 |
|------|------|------|
| 开发环境 | 4核8G | 日常开发 |
| 测试环境 | 8核16G | 功能测试、性能测试 |
| 预生产环境 | 8核16G | 最终验证 |
| 生产环境 | 16核32G | 正式运行 |

### 7.4 详细时间线（甘特图）

```
RBAC升级项目时间线
================

阶段一：数据模型建设（第1-2周）
├── 数据库表设计 (第1周)
├── 实体类创建 (第1周)
├── Mapper接口 (第1周)
└── 数据初始化 (第2周)

阶段二：权限加载链路改造（第3-4周）
├── 用户详情服务改造 (第3周)
├── JWT Token处理 (第3周)
├── 权限缓存实现 (第4周)
└── 权限预热策略 (第4周)

阶段三：权限校验启用（第5-6周）
├── 方法级安全注解 (第5周)
├── URL级权限规则 (第5周)
├── 自定义权限校验器 (第6周)
└── AOP切面实现 (第6周)

阶段四：管理接口建设（第7-9周）
├── 角色管理API (第7周)
├── 权限管理API (第7周)
├── 用户角色分配 (第8周)
└── 前端管理界面 (第8-9周)

阶段五：高级特性实现（第10-13周）
├── 角色继承机制 (第10周)
├── 数据权限控制 (第11周)
├── 权限缓存优化 (第12周)
└── 审计日志增强 (第13周)

测试与上线（第14-16周）
├── 单元测试 (第14周)
├── 集成测试 (第15周)
├── 安全测试 (第15周)
├── 性能测试 (第16周)
└── 上线部署 (第16周)
```

### 7.5 资源分配表

| 阶段 | 后端开发 | 前端开发 | DBA | 测试 |
|------|----------|----------|-----|------|
| 阶段一 | 2人 | 0人 | 1人 | 0人 |
| 阶段二 | 2人 | 0人 | 0人 | 0人 |
| 阶段三 | 2人 | 0人 | 0人 | 0人 |
| 阶段四 | 1人 | 1人 | 0人 | 0人 |
| 阶段五 | 2人 | 0人 | 1人 | 0人 |
| 测试阶段 | 1人 | 0人 | 0人 | 1人 |

---

## 8. 风险评估

### 8.1 技术风险

| 风险 | 概率 | 影响 | 应对措施 |
|------|------|------|----------|
| 数据迁移失败 | 中 | 高 | 1. 完整备份<br>2. 分步迁移<br>3. 回滚方案 |
| 性能下降 | 中 | 中 | 1. 缓存优化<br>2. 查询优化<br>3. 性能测试 |
| 兼容性问题 | 低 | 中 | 1. 充分测试<br>2. 渐进式升级<br>3. 版本控制 |
| 安全漏洞 | 低 | 高 | 1. 安全审计<br>2. 渗透测试<br>3. 代码审查 |

### 8.2 业务风险

| 风险 | 概率 | 影响 | 应对措施 |
|------|------|------|----------|
| 权限配置错误 | 中 | 高 | 1. 权限模板<br>2. 配置验证<br>3. 审批流程 |
| 业务中断 | 低 | 高 | 1. 灰度发布<br>2. 快速回滚<br>3. 监控告警 |
| 用户体验下降 | 低 | 中 | 1. 用户测试<br>2. 反馈收集<br>3. 优化改进 |

### 8.3 安全风险

| 风险 | 概率 | 影响 | 应对措施 |
|------|------|------|----------|
| 权限提升 | 低 | 高 | 1. 最小权限原则<br>2. 定期审计<br>3. 权限回收 |
| 数据泄露 | 低 | 高 | 1. 数据加密<br>2. 访问控制<br>3. 审计日志 |
| 身份伪造 | 低 | 高 | 1. 强认证机制<br>2. Token安全<br>3. 会话管理 |

### 8.4 资源风险

| 风险 | 概率 | 影响 | 应对措施 |
|------|------|------|----------|
| 人员不足 | 中 | 中 | 1. 外包支持<br>2. 培训提升<br>3. 优先级调整 |
| 时间紧张 | 中 | 中 | 1. 范围控制<br>2. 并行开发<br>3. 自动化测试 |
| 预算超支 | 低 | 中 | 1. 成本控制<br>2. 资源优化<br>3. 分期实施 |

### 8.5 风险矩阵

```
风险矩阵
========

影响程度 ↑
        高 | 3.安全漏洞    | 1.数据迁移失败 |
          |              |              |
        中 | 2.性能下降    | 4.权限配置错误 |
          |              |              |
        低 |              | 5.人员不足    |
          +----------------------------------------→
            低           中           高
                      发生概率
```

### 8.6 风险应对措施汇总

1. **数据迁移风险**
   - 完整备份现有数据
   - 分步迁移，每步验证
   - 准备回滚脚本
   - 迁移后数据完整性检查

2. **性能风险**
   - 权限数据缓存
   - 数据库查询优化
   - 性能测试和监控
   - 渐进式上线

3. **安全风险**
   - 安全代码审查
   - 渗透测试
   - 最小权限原则
   - 审计日志记录

4. **业务风险**
   - 灰度发布
   - 快速回滚机制
   - 用户反馈收集
   - 持续优化改进

---

## 附录

### A. 参考资料

1. [Spring Security 官方文档](https://spring.io/projects/spring-security)
2. [RBAC 模型设计](https://en.wikipedia.org/wiki/Role-based_access_control)
3. [JWT 安全最佳实践](https://tools.ietf.org/html/rfc7519)
4. [MyBatis 官方文档](https://mybatis.org/mybatis-3/)

### B. 术语表

| 术语 | 说明 |
|------|------|
| RBAC | 基于角色的访问控制 |
| JWT | JSON Web Token |
| AOP | 面向切面编程 |
| CRUD | 增删改查 |

### C. 变更记录

| 版本 | 日期 | 作者 | 变更说明 |
|------|------|------|----------|
| 1.0 | 2026-05-11 | AI助手 | 初始版本 |

---

**文档生成时间**: 2026-05-11 22:16  
**分析项目**: riveroll-workbench-web  
**当前RBAC评分**: 68/100  
**升级目标**: 90/100  
**预计工期**: 16周  
**总预算**: 待估算