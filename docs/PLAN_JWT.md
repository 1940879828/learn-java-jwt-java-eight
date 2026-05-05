# JWT 双 Token 认证方案实施计划

## Context

项目 `jwt-java-eight` 是一个 Spring Boot 2.7.6 + Java 8 的空壳项目，仅有 `spring-boot-starter-security` 依赖。需要构建一套完整的 JWT 双 Token（Access + Refresh）认证系统，使用 MyBatis + MySQL 持久化，遵循阿里巴巴 Java 开发手册。

## 阶段一：依赖补全

**修改文件**: `pom.xml`

在 `<dependencies>` 中追加：

```xml
<!-- JJWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>

<!-- Database -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.2.2</version>
</dependency>

<!-- Utils -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.22</version>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

## 阶段二：数据库建表

**Mysql数据库版本**：5.7.44

**新建文件**: `docs/sql/init_jwt.sql`

两张表：
- `sys_user`: id(BIGINT PK AUTO_INCREMENT), username(VARCHAR(64) UNIQUE NOT NULL), password(VARCHAR(128) NOT NULL), status(TINYINT DEFAULT 1), create_time(DATETIME DEFAULT CURRENT_TIMESTAMP)
- `sys_refresh_token`: id(BIGINT PK AUTO_INCREMENT), user_id(BIGINT NOT NULL), token_hash(VARCHAR(64) NOT NULL), expire_time(DATETIME NOT NULL), device_info(VARCHAR(255)), jti_id(VARCHAR(64) NOT NULL), create_time(DATETIME DEFAULT CURRENT_TIMESTAMP)
- 索引：`idx_user_id` on sys_refresh_token(user_id), `idx_jti` on sys_refresh_token(jti_id), `idx_expire` on sys_refresh_token(expire_time)

## 阶段三：配置文件

**新建文件**: `src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/jwt_java_eight?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: org.example.jwtjavaeight.domain.entity
  configuration:
    map-underscore-to-camel-case: true

jwt:
  secret: YourSecretKeyHereMustBeAtLeast256BitsLongForHS256Algorithm
  access-token-expiration: 900000      # 15 分钟
  refresh-token-expiration: 604800000  # 7 天
  token-prefix: "Bearer "
  header: Authorization
```

## 阶段四：包结构与代码文件清单

基础包: `org.example.jwtjavaeight`

```
├── common/
│   ├── Result.java                  # 统一响应包装 {code, message, data}
│   └── ResultCode.java              # 响应码常量枚举 (200/401/403/500)
├── config/
│   ├── SecurityConfig.java          # SecurityFilterChain Bean 配置
│   ├── JwtConfig.java               # JWT 配置属性注入 (@ConfigurationProperties)
│   └── SecurityExceptionHandler.java # 处理 401/403，返回 JSON Result
├── constants/
│   └── SecurityConstants.java       # 所有硬编码字符串常量
├── domain/
│   ├── entity/
│   │   ├── SysUser.java
│   │   └── SysRefreshToken.java
│   └── dto/
│       ├── LoginRequest.java        # @NotBlank username, password
│       ├── LoginResponse.java       # accessToken, refreshToken, tokenType
│       └── RefreshRequest.java      # refreshToken
├── mapper/
│   ├── UserMapper.java              # MyBatis Mapper 接口
│   ├── RefreshTokenMapper.java
│   └── (对应的 mapper/*.xml)
├── security/
│   ├── JwtAuthenticationFilter.java # OncePerRequestFilter
│   ├── JwtUserDetails.java          # 实现 UserDetails
│   └── UserDetailsServiceImpl.java  # 实现 UserDetailsService
├── service/
│   ├── AuthService.java             # 登录、刷新、登出
│   └── impl/
│       └── AuthServiceImpl.java
├── utils/
│   ├── JwtUtil.java                 # 生成/解析/校验 Token，提取 JTI
│   └── HashUtil.java                # Hutool SecureUtil.sha256
├── controller/
│   └── AuthController.java          # POST /auth/login, POST /auth/refresh, POST /auth/logout
└── JwtJavaEightApplication.java     # 已有
```

## 阶段五：核心类设计要点

### Result.java — 统一响应包装

```java
@Data
public class Result<T> implements Serializable {
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) { ... }
    public static <T> Result<T> success() { ... }
    public static <T> Result<T> failure(Integer code, String message) { ... }
}
```

前端统一按 `{ "code": 200, "message": "success", "data": { ... } }` 格式处理。

### ResultCode.java — 响应码枚举

```java
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "没有权限"),
    ERROR(500, "系统异常");
}
```

### SecurityExceptionHandler.java — 未登录/无权限返回 JSON

- 实现 `AuthenticationEntryPoint`（未登录 → 401）：
  ```java
  response.setContentType("application/json;charset=UTF-8");
  response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  response.getWriter().write(JSONUtil.toJsonStr(Result.failure(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage())));
  ```

- 实现 `AccessDeniedHandler`（无权限 → 403）：
  ```java
  response.setContentType("application/json;charset=UTF-8");
  response.setStatus(HttpServletResponse.SC_FORBIDDEN);
  response.getWriter().write(JSONUtil.toJsonStr(Result.failure(ResultCode.FORBIDDEN.getCode(), ResultCode.FORBIDDEN.getMessage())));
  ```

- 在 `SecurityConfig` 中通过 `.exceptionHandling()` 注册：
  ```java
  .exceptionHandling()
      .authenticationEntryPoint(securityExceptionHandler)
      .accessDeniedHandler(securityExceptionHandler)
  ```

- 同时在 `JwtAuthenticationFilter` 中捕获 Token 异常时，直接写出 401 JSON 响应并 `return`，不再传递给后续过滤器。

### SecurityConfig.java
- 使用 `@Configuration` + `@EnableWebSecurity`（Boot 2.7 写法）
- 定义 `SecurityFilterChain` Bean：禁用 CSRF，开启 CORS，放行 `/auth/**`，其余需认证
- 注册 `JwtAuthenticationFilter` 在 `UsernamePasswordAuthenticationFilter` 之前
- 通过 `.exceptionHandling()` 注册 `SecurityExceptionHandler`（401/403 返回 JSON Result）
- 定义 `PasswordEncoder` Bean 为 `BCryptPasswordEncoder`

### JwtConfig.java
- `@ConfigurationProperties(prefix = "jwt")` 读取 application.yml 中的 jwt 配置
- 字段：secret, accessTokenExpiration, refreshTokenExpiration, tokenPrefix, header

### JwtUtil.java
- `generateAccessToken(userId, username, authorities)` — 15min 过期，携带 claims，包含 JTI (UUID)
- `generateRefreshToken(userId)` — 7天过期，仅含 userId + JTI
- `parseToken(token)` — 解析 Claims
- `validateToken(token)` — 校验签名和过期
- `getJtiFromToken(token)` — 提取 JTI
- `getUserIdFromToken(token)` — 提取 userId
- 使用 `Jwts.parserBuilder().setSigningKey(secretBytes).build()` 解析

### JwtAuthenticationFilter.java
- 继承 `OncePerRequestFilter`
- 从 Header 提取 Token → 验证 → 设置 `SecurityContextHolder`
- Token 过期/无效时，直接写出 401 JSON 响应 `Result.failure(401, "Token已过期或无效")`，清空 Context，不再传递请求

### AuthServiceImpl.java
- `login(LoginRequest)`:
  1. 通过 `UserDetailsServiceImpl` 认证用户
  2. 生成 Access Token + Refresh Token
  3. 将 Refresh Token SHA256 后存入 `sys_refresh_token`（关联设备信息和 JTI）
  4. 返回 `LoginResponse`
- `refresh(RefreshRequest)`:
  1. 解析传入的 Refresh Token，提取 userId 和 JTI
  2. 在数据库中查找对应记录（SHA256 匹配）
  3. 校验过期时间
  4. 删除旧记录，生成新的双 Token 对
  5. 存储新 Refresh Token hash，返回新 Token
- `logout(token)`:
  1. 从 Access Token 提取 userId
  2. 删除该用户对应设备的 Refresh Token 记录

## 阶段六：实施顺序

1. 修改 `pom.xml` 添加依赖
2. 创建 `application.yml` 配置
3. 编写 `docs/sql/init.sql` 建表语句
4. 编写常量类 `SecurityConstants.java`
5. 编写 `common/Result.java` + `common/ResultCode.java` 统一响应
6. 编写 `domain.entity` 和 `domain.dto`
7. 编写 `UserMapper` / `RefreshTokenMapper` 接口及 XML
8. 编写 `JwtConfig` 配置属性类
9. 编写 `JwtUtil` 工具类
10. 编写 `HashUtil` 工具类
11. 编写 `JwtUserDetails` / `UserDetailsServiceImpl`
12. 编写 `SecurityExceptionHandler`（401/403 返回 JSON）
13. 编写 `JwtAuthenticationFilter`
14. 编写 `SecurityConfig`（注册 Filter + ExceptionHandler）
15. 编写 `AuthService` / `AuthServiceImpl`
16. 编写 `AuthController`（所有接口返回 `Result<T>`）
17. 编写单元测试

## 验证方式

1. `mvn clean compile` 确认编译通过
2. 启动 MySQL，执行 `init.sql`
3. `mvn spring-boot:run` 启动应用
4. 测试登录：`curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"123456"}'`
5. 测试刷新：用返回的 refreshToken 调用 `/auth/refresh`
6. 测试登出：携带 accessToken 调用 `/auth/logout`
7. `mvn test` 运行测试
