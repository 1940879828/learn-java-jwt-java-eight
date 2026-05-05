# JWT Java Eight

基于 Spring Boot 2.7.6 + Java 8 的 JWT 双 Token 认证系统。

## 技术栈

- Java 8
- Spring Boot 2.7.6
- Spring Security 5.7.5
- MySQL 5.7.44
- JWT (Access Token + Refresh Token)
- Swagger / OpenAPI 3

## 构建与运行

```bash
# 编译
mvn clean package

# 运行
mvn spring-boot:run

# 测试
mvn test

# 运行单个测试
mvn test -Dtest=JwtJavaEightApplicationTests
```

## API 文档

应用启动后，访问 Swagger UI：

```
http://localhost:8080/swagger-ui/index.html
```

### Schemas 导出端点

为了方便将项目的数据结构导出给 AI 或其他工具分析，提供了以下专用端点：

#### 1. JSON 格式的 Schemas（推荐复制给 AI）

```
GET http://localhost:8080/api/doc/schemas
```

返回完整的 Schemas 定义（JSON 格式），包含所有字段类型、描述、必填项等详细信息。

**响应示例：**
```json
{
  "title": "项目数据对象结构 (Schemas)",
  "totalCount": 8,
  "schemas": {
    "LoginRequest": { ... },
    "LoginResponse": { ... },
    ...
  },
  "usage": "复制 schemas 字段内容给 AI，让其了解项目的数据结构"
}
```

#### 2. Markdown 格式的 Schemas（最易读）

```
GET http://localhost:8080/api/doc/schemas-simple
```

返回 Markdown 格式的精简版本，更适合人类阅读和直接复制。

**响应示例：**
```markdown
# 项目数据对象结构

共 8 个数据对象

---

## LoginRequest

字段:
- **username** (string): 用户名
- **password** (string): 密码

必填字段: username, password

---
...
```

#### 3. 完整的 OpenAPI 文档

```
GET http://localhost:8080/api/doc/full
```

返回完整的 OpenAPI 文档（包含所有端点定义和 schemas）。

### 使用流程

1. 启动应用
2. 浏览器访问 `http://localhost:8080/api/doc/schemas-simple`
3. 全选复制（Ctrl+A → Ctrl+C）
4. 粘贴给 AI 或保存到文档

## 主要功能

- 用户登录认证（Access Token + Refresh Token）
- Token 刷新机制
- 用户注册
- 登录失败次数限制与账户锁定
- 登录审计日志
- 基于 JWT 的无状态认证

## 包结构

所有代码位于 `org.example.jwtjavaeight` 包下：

- `config` - 配置类（Spring Security、OpenAPI 等）
- `controller` - REST 控制器
- `security` - 安全相关（Filter、Provider、UserDetails 等）
- `domain` - 数据模型（entity、dto）
- `mapper` - MyBatis Mapper 接口
- `service` - 业务逻辑层
- `exception` - 自定义异常
- `constants` - 常量定义
- `common` - 公共类（Result、ResultCode 等）

## 安全特性

- 密码 BCrypt 加密存储
- 登录失败次数限制（默认 5 次）
- 账户自动锁定（默认 2 小时）
- 锁定到期自动解锁
- JWT Token 双重验证（Access + Refresh）
- 完整的登录审计日志

## 数据库

MySQL 5.7.44，主要表：

- `sys_user` - 用户表
- `sys_refresh_token` - Refresh Token 表
- `sys_login_log` - 登录日志表

## 开发指南

详细的开发指南请参考 [CLAUDE.md](./CLAUDE.md)
