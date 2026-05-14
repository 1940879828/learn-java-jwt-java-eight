# 代码审查报告

**审查日期**: 2026-05-14  
**项目**: JWT Java Eight (Spring Boot 2.7.6 + JWT)  
**审查范围**: 全项目代码、配置、安全性

---

## 🔴 严重问题（Critical）

### 1. ⚠️ JWT密钥泄露风险

**文件**: `src/main/resources/application.yml:18`

```yaml
jwt:
  secret: YourSecretKeyHereMustBeAtLeast256BitsLongForHS256Algorithm
```

**问题**:
- JWT密钥以明文形式存储在配置文件中
- 该文件可能被提交到Git仓库

**风险**:
- 攻击者获取密钥后可伪造任意用户的JWT token
- 可绕过所有权限控制

**建议**:
```yaml
# 使用环境变量或外部配置
jwt:
  secret: ${JWT_SECRET:default-dev-key-only-for-local}
```

```bash
# 生产环境通过环境变量设置
export JWT_SECRET=$(openssl rand -base64 64)
```

**优先级**: 🔴 立即修复

---

### 2. ⚠️ 数据库密码明文存储

**文件**: `src/main/resources/application.yml:8`

```yaml
spring:
  datasource:
    password: 123456
```

**问题**:
- 数据库密码明文存储在配置文件中

**建议**:
```yaml
spring:
  datasource:
    password: ${DB_PASSWORD:123456}  # 默认值仅用于开发环境
```

**优先级**: 🔴 生产环境必须修复

---

### 3. ⚠️ CORS配置过于宽松

**文件**: `src/main/java/org/example/jwtjavaeight/config/SecurityConfig.java:120`

```java
configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
```

**问题**:
- 允许所有来源的跨域请求
- 可能导致CSRF攻击

**建议**:
```java
// 生产环境明确指定允许的域名
configuration.setAllowedOriginPatterns(
    Arrays.asList(
        "https://yourdomain.com",
        "https://admin.yourdomain.com"
    )
);

// 或从配置文件读取
@Value("${cors.allowed-origins}")
private String[] allowedOrigins;
```

**优先级**: 🔴 生产环境必须修复

---

## 🟡 高优先级问题（High）

### 4. 🔒 SQL注入风险（部分）

**文件**: `src/main/resources/mapper/UserMapper.xml:90-92`

```xml
<if test="filter.keyword != null and filter.keyword != ''">
    AND (username LIKE CONCAT('%', #{filter.keyword}, '%')
         OR email LIKE CONCAT('%', #{filter.keyword}, '%'))
</if>
```

**分析**:
- ✅ 使用了MyBatis的`#{}`参数化查询，**不存在SQL注入风险**
- ✅ 所有Mapper文件都正确使用了参数绑定

**建议**: 继续保持，避免使用`${}`字符串拼接

---

### 5. 🔐 密码复杂度未验证

**文件**: `src/main/java/org/example/jwtjavaeight/domain/dto/LoginRequest.java`

```java
@NotBlank(message = "密码不能为空")
private String password;
```

**问题**:
- 注册/修改密码时未验证密码复杂度
- 允许弱密码（如"123456"）

**建议**:
```java
@Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
    message = "密码必须包含大小写字母、数字和特殊字符，长度至少8位"
)
private String password;
```

**优先级**: 🟡 建议实施

---

### 6. 🚫 缺少Rate Limiting（速率限制）

**文件**: 登录接口未实现速率限制

**问题**:
- 攻击者可对登录接口发起暴力破解
- 虽然有账户锁定机制（5次失败锁定2小时），但未限制请求频率

**建议**:
```java
// 1. 添加依赖
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
</dependency>

// 2. 创建RateLimitFilter
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    // 每IP每分钟最多10次登录尝试
    private final LoadingCache<String, Bucket> cache;
    
    @Override
    protected void doFilterInternal(...) {
        if (request.getRequestURI().equals("/api/v1/auth/login")) {
            String ip = getClientIp(request);
            if (!cache.get(ip).tryConsume(1)) {
                response.setStatus(429); // Too Many Requests
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
```

**优先级**: 🟡 生产环境强烈建议

---

### 7. ⏰ Token过期时间过短

**文件**: `application.yml:19`

```yaml
access-token-expiration: 900000  # 15 分钟
```

**问题**:
- Token有效期仅15分钟，用户体验较差
- 前端需要频繁刷新token

**建议**:
```yaml
# 方案1: 延长access token有效期
access-token-expiration: 3600000  # 1小时

# 方案2: 实现"记住我"功能（已添加remember字段）
# LoginSuccessHandler中根据remember字段生成不同有效期的token
if (remember) {
    expiration = 7天;
} else {
    expiration = 1小时;
}
```

**优先级**: 🟡 用户体验优化

---

## 🟢 中等优先级问题（Medium）

### 8. 📝 日志可能泄露敏感信息

**文件**: `src/main/java/org/example/jwtjavaeight/security/handler/LoginFailureHandler.java:96`

```java
log.info("[LoginFailure] 登录失败, username: {}, ip: {}, reason: {}",
    username, clientIp, exception.getMessage());
```

**问题**:
- 日志包含失败原因，可能泄露用户存在性
- 攻击者可通过日志判断用户名是否存在

**建议**:
```java
// 对外统一返回"用户名或密码错误"
// 内部日志记录详细信息，但控制访问权限
log.warn("[LoginFailure] 登录失败, username: {}, ip: {}", username, clientIp);
// 详细原因只在DEBUG级别记录
log.debug("[LoginFailure] 失败原因: {}", exception.getMessage());
```

**优先级**: 🟢 建议改进

---

### 9. 🔍 异常信息暴露过多

**文件**: `src/main/java/org/example/jwtjavaeight/exception/GlobalExceptionHandler.java:60`

```java
log.error("Unexpected error [traceId={}]: ", traceId, ex);
return ResponseEntity.status(500)
    .body(Result.error(ErrorCode.INTERNAL_ERROR, "系统异常，请联系管理员"));
```

**分析**:
- ✅ 对外返回通用错误信息，未暴露堆栈
- ✅ 详细错误记录在日志中
- ✅ 包含traceId方便追踪

**建议**: 保持当前做法，已经符合最佳实践

---

### 10. 🗂️ 缺少索引优化建议

**文件**: SQL表结构

**建议添加的索引**:
```sql
-- sys_login_log表
CREATE INDEX idx_login_time ON sys_login_log(login_time);
CREATE INDEX idx_status ON sys_login_log(status);

-- sys_user表
CREATE INDEX idx_status ON sys_user(status);
CREATE INDEX idx_lock_time ON sys_user(lock_time);

-- sys_refresh_token表
CREATE INDEX idx_expire_time ON sys_refresh_token(expire_time);
```

**优先级**: 🟢 性能优化

---

### 11. 🧪 缺少单元测试

**当前状态**:
- 存在测试文件但内容较少
- 核心业务逻辑缺少测试覆盖

**建议**:
```java
// 关键测试场景
@Test
void testLoginWithInvalidCredentials() { }

@Test
void testAccountLockAfterMaxAttempts() { }

@Test
void testJwtTokenValidation() { }

@Test
void testPasswordEncryption() { }

@Test
void testPermissionCheck() { }
```

**优先级**: 🟢 代码质量提升

---

## ✅ 优秀实践（Good Practices）

### 1. ✨ 安全措施完善

- ✅ 使用BCrypt加密密码
- ✅ 实现登录失败锁定机制
- ✅ 记录登录审计日志
- ✅ JWT token包含过期时间
- ✅ Refresh Token存储哈希值
- ✅ 使用参数化查询防SQL注入

### 2. 📊 异常处理规范

- ✅ 全局异常处理器
- ✅ 统一错误返回格式
- ✅ 详细的字段校验错误提示
- ✅ TraceId追踪请求

### 3. 🏗️ 代码结构清晰

- ✅ 分层架构明确（Controller - Service - Mapper）
- ✅ DTO和Entity分离
- ✅ 合理使用枚举类
- ✅ 统一Result返回格式

### 4. 📝 日志记录完整

- ✅ 关键操作都有日志
- ✅ 使用SLF4J统一日志接口
- ✅ 日志级别使用合理

### 5. 🎨 代码规范

- ✅ 使用Lombok减少样板代码
- ✅ 注释完整
- ✅ 命名规范清晰
- ✅ Swagger API文档

---

## 📋 改进建议优先级

### 立即修复（Production Blockers）
1. JWT密钥环境变量化
2. 数据库密码环境变量化
3. CORS配置限制

### 高优先级（Before Production）
4. 密码复杂度验证
5. 速率限制实现
6. Token有效期优化

### 中优先级（Quality Improvements）
7. 日志敏感信息脱敏
8. 数据库索引优化
9. 单元测试补充

### 低优先级（Nice to Have）
10. 配置文件环境分离（dev/test/prod）
11. 健康检查接口
12. 监控指标暴露
13. Docker化部署

---

## 🔧 快速修复清单

### 1. 环境变量配置

创建 `.env.example`:
```bash
# 数据库配置
DB_URL=jdbc:mysql://localhost:3306/jwt_java_eight
DB_USERNAME=root
DB_PASSWORD=your_password_here

# JWT配置
JWT_SECRET=your_256_bit_secret_here

# CORS配置
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://admin.yourdomain.com
```

### 2. application.yml更新

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/jwt_java_eight}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:123456}

jwt:
  secret: ${JWT_SECRET:YourSecretKeyHereMustBeAtLeast256BitsLongForHS256Algorithm}

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}
```

### 3. SecurityConfig更新

```java
@Value("${cors.allowed-origins}")
private String allowedOriginsStr;

@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    List<String> origins = Arrays.asList(allowedOriginsStr.split(","));
    configuration.setAllowedOriginPatterns(origins);
    // ...
}
```

---

## 📊 代码质量评分

| 维度 | 评分 | 说明 |
|------|------|------|
| **安全性** | 7/10 | 基础安全措施完善，但配置管理需加强 |
| **代码规范** | 9/10 | 结构清晰，命名规范，注释完整 |
| **性能** | 7/10 | 基础功能正常，需要索引优化 |
| **可维护性** | 8/10 | 分层清晰，易于理解和修改 |
| **测试覆盖** | 4/10 | 缺少完整的单元测试 |
| **文档** | 8/10 | Swagger文档完善，代码注释清晰 |

**综合评分**: 7.2/10

---

## 🎯 总结

### 优点
1. ✅ 安全基础扎实（BCrypt、JWT、参数化查询）
2. ✅ 代码结构规范（分层清晰、命名合理）
3. ✅ 异常处理完善（全局处理器、统一格式）
4. ✅ 日志记录详细（审计日志、操作日志）

### 需要改进
1. 🔴 敏感配置管理（密钥、密码环境变量化）
2. 🟡 生产环境安全（CORS限制、速率限制）
3. 🟢 代码质量（单元测试、性能优化）

### 建议
- **生产环境部署前**: 必须修复所有红色严重问题
- **近期优化**: 实现黄色高优先级改进
- **长期规划**: 补充测试、优化性能、完善监控

---

**审查人**: Claude Code  
**下次审查建议**: 修复严重问题后
