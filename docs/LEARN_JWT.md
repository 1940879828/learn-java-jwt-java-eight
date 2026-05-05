# JWT 双 Token 认证机制 — Spring Boot 实战教程

> **写在前面**
>
> 这份教程将带领你从零开始，使用 Java 8 + Spring Boot 2.7 + Spring Security 构建一套完整的 JWT 双 Token 认证系统。
> 本教程基于 **jwt-java-eight** 项目的实际代码编写，采用 **Filter 链标准流程** 进行认证，而非传统的 Controller 手动驱动方式。
>
> 我不会让你"复制粘贴就跑起来"，而是让你真正理解**每一行代码为什么要这么写**。
>
> 请耐心阅读，跟着步骤动手，遇到报错先思考再查资料。这就是工程师成长的方式。
>
> **项目状态：✅ 生产就绪** - 本项目已完成 Filter 链认证改造，所有代码均已在开发环境验证通过。

---

## 目录

- [1. 项目概述与环境准备](#1-项目概述与环境准备)
- [2. JWT 核心原理剖析](#2-jwt-核心原理剖析)
- [3. 实战：引入依赖与配置](#3-实战引入依赖与配置)
- [4. 核心模块开发](#4-核心模块开发)
- [5. Spring Security 配置整合](#5-spring-security-配置整合)
- [6. 双 Token 刷新机制实现](#6-双-token-刷新机制实现)
- [7. 接口测试与调试](#7-接口测试与调试)
- [8. 总结与面试考点](#8-总结与面试考点)

---

## 1. 项目概述与环境准备

### 1.1 我们要解决什么问题？（Session vs JWT）

在传统的 Web 应用中，用户登录后，服务器会创建一个 **Session** 并将 Session ID 通过 Cookie 返回给浏览器。每次请求时浏览器自动带上 Cookie，服务器根据 Session ID 查找对应的用户信息。

**Session 方案的问题：**

| 问题 | 说明 |
|------|------|
| 服务端有状态 | Session 存储在服务器内存中，重启丢失 |
| 分布式困难 | 多台服务器之间 Session 不共享，需要 Session 同步或集中存储 |
| CORS 限制 | Cookie 有跨域限制，前后端分离项目使用不便 |
| 移动端不友好 | App 端管理 Cookie 不方便 |

**JWT 方案的优势：**

| 优势 | 说明 |
|------|------|
| 服务端无状态 | Token 自包含用户信息，服务器不需要存储 |
| 分布式友好 | 任何服务节点都能独立验证 Token |
| 跨域友好 | Token 放在 Header 中，不受 Cookie 跨域限制 |
| 移动端友好 | 客户端自行管理 Token 字符串即可 |

> **一句话总结**：Session 是"服务器记住你是谁"，JWT 是"你自己带着身份证明来"。

### 1.2 环境清单

在开始之前，请确保你的开发环境满足以下要求：

| 工具 | 版本要求 | 说明 |
|------|---------|------|
| JDK | **1.8** (Java 8) | 本项目严格使用 Java 8 |
| Maven | 3.6+ | 依赖管理与构建 |
| MySQL | 5.7+ | 数据持久化 |
| IDE | IntelliJ IDEA（推荐） | 也可使用 VS Code + Java 插件 |
| Postman / curl | 任意版本 | 接口测试工具 |

### 1.3 项目结构说明

本项目（**jwt-java-eight**）已经完成初始化和开发，采用 **Filter 链标准流程** 处理登录认证。

**实际项目结构：**

```
jwt-java-eight/
├── pom.xml                      # Maven 配置文件
├── src/
│   ├── main/
│   │   ├── java/org/example/jwtjavaeight/
│   │   │   ├── JwtJavaEightApplication.java     # 启动类
│   │   │   ├── common/                          # 通用响应类（Result, ResultCode）
│   │   │   ├── config/                          # 配置类
│   │   │   │   ├── SecurityConfig.java          # Spring Security 核心配置
│   │   │   │   ├── SecurityExceptionHandler.java # 安全异常处理器
│   │   │   │   ├── JwtConfig.java               # JWT 配置属性
│   │   │   │   ├── OpenApiConfig.java           # Swagger 配置
│   │   │   │   └── StartupInfoConfig.java       # 启动信息打印
│   │   │   ├── constants/                       # 常量定义
│   │   │   │   └── SecurityConstants.java       # 安全相关常量
│   │   │   ├── controller/                      # 控制器层
│   │   │   │   └── AuthController.java          # 认证接口（register/refresh/logout）
│   │   │   ├── domain/                          # 数据对象
│   │   │   │   ├── dto/                         # 数据传输对象
│   │   │   │   │   ├── LoginRequest.java        # 登录请求 DTO
│   │   │   │   │   ├── LoginResponse.java       # 登录响应 DTO
│   │   │   │   │   ├── RegisterRequest.java     # 注册请求 DTO
│   │   │   │   │   └── RefreshRequest.java      # 刷新 Token 请求 DTO
│   │   │   │   └── entity/                      # 数据库实体
│   │   │   │       ├── SysUser.java             # 用户表实体
│   │   │   │       └── SysRefreshToken.java     # 刷新令牌表实体
│   │   │   ├── exception/                       # 异常定义
│   │   │   │   ├── BusinessException.java       # 业务异常基类
│   │   │   │   ├── GlobalExceptionHandler.java  # 全局异常处理器
│   │   │   │   ├── TokenExpiredException.java   # Token 过期异常
│   │   │   │   ├── UsernameExistsException.java # 用户名已存在异常
│   │   │   │   └── UserDisabledException.java   # 用户被禁用异常
│   │   │   ├── mapper/                          # MyBatis Mapper 接口
│   │   │   │   ├── UserMapper.java              # 用户表 Mapper
│   │   │   │   └── RefreshTokenMapper.java      # 刷新令牌表 Mapper
│   │   │   ├── security/                        # 安全相关（核心）
│   │   │   │   ├── JwtAuthenticationFilter.java # JWT 鉴权过滤器
│   │   │   │   ├── JwtLoginFilter.java          # JWT 登录过滤器（新增）
│   │   │   │   ├── JwtAuthToken.java            # 自定义认证令牌（新增）
│   │   │   │   ├── JwtAuthenticationProvider.java # 自定义认证提供者（新增）
│   │   │   │   ├── JwtUserDetails.java          # JWT 用户详情
│   │   │   │   ├── UserDetailsServiceImpl.java  # UserDetailsService 实现
│   │   │   │   └── handler/                     # 认证处理器（新增）
│   │   │   │       ├── LoginSuccessHandler.java # 登录成功处理器
│   │   │   │       └── LoginFailureHandler.java # 登录失败处理器
│   │   │   ├── service/                         # 业务逻辑层
│   │   │   │   ├── AuthService.java             # 认证服务接口
│   │   │   │   └── impl/
│   │   │   │       └── AuthServiceImpl.java     # 认证服务实现
│   │   │   └── utils/                           # 工具类
│   │   │       ├── JwtUtil.java                 # JWT 工具类
│   │   │       └── HashUtil.java                # 哈希工具类
│   │   └── resources/
│   │       ├── application.yml                  # 应用配置
│   │       └── mapper/                          # MyBatis XML
│   │           ├── UserMapper.xml
│   │           └── RefreshTokenMapper.xml
│   └── test/                                    # 测试代码
└── docs/                                        # 文档目录
    ├── sql/init_jwt.sql                         # 数据库初始化脚本
    ├── LEARN_JWT.md                             # 本教程
    └── JWTFilterLogin.md                        # Filter 链改造记录
```

> **设计理念**：本项目采用 **Filter 链标准流程** 处理登录认证，而非传统的 Controller 手动驱动方式。这是企业级项目的推荐做法，职责更清晰，易于维护和扩展。登录请求由 `JwtLoginFilter` 在 Filter 链中自动拦截处理，`AuthController` 不参与登录逻辑。

---

## 2. JWT 核心原理剖析

### 2.1 什么是 JWT？结构拆解

JWT（JSON Web Token）是一个开放标准（RFC 7519），它定义了一种紧凑且自包含的方式，用于在各方之间安全地传输信息。

一个 JWT 由三部分组成，用 `.` 分隔：

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInVzZXJJZCI6MSwiYXV0aG9yaXRpZXMiOiJST0xFX1VTRVIiLCJpYXQiOjE3MTQ1NjM2MDAsImV4cCI6MTcxNDU2NDUwMH0.K8j2xZQwE...
│_______Header_______│ │_________________________________Payload__________________________________│ │___Signature___│
```

**三段结构详解：**

#### Header（头部）

```json
{
  "alg": "HS256",   // 签名算法
  "typ": "JWT"      // Token 类型
}
```

Header 经过 **Base64Url** 编码后成为第一段。它告诉验证方"我用什么算法签的名"。

#### Payload（载荷）

```json
{
  "jti": "550e8400-e29b-41d4-a716-446655440000",   // JWT唯一标识
  "sub": "admin",                                   // 主题（用户名）
  "userId": 1,                                      // 自定义声明：用户ID
  "authorities": "ROLE_USER",                       // 自定义声明：权限
  "iat": 1714563600,                                // 签发时间
  "exp": 1714564500                                 // 过期时间
}
```

Payload 包含了**声明（Claims）**，分为三类：
- **注册声明**：`iss`（签发者）、`sub`（主题）、`exp`（过期时间）、`iat`（签发时间）、`jti`（唯一ID）
- **公共声明**：自定义但建议在 IANA 注册的声明
- **私有声明**：我们自定义的业务数据，如 `userId`、`authorities`

> **⚠️ 安全警告**：Payload 只是 Base64 编码，**不是加密**！任何人都能解码看到内容。所以绝对不要在 Payload 中放密码、手机号等敏感信息。

#### Signature（签名）

```
HMACSHA256(
  Base64Url(Header) + "." + Base64Url(Payload),
  secret
)
```

签名的作用是**防篡改**。服务器用只有自己知道的密钥对前两段进行签名，如果有人修改了 Payload 中的内容（比如把 userId 改成别人的），签名验证就会失败。

### 2.2 为什么要用双 Token？

**单 Token 的困境：**

如果只有一个 Token，你会面临一个两难选择：

| 方案 | 有效期 | 安全性 | 用户体验 |
|------|--------|--------|---------|
| 短有效期 | 15分钟 | ✅ 高（被盗后很快失效） | ❌ 用户频繁重新登录 |
| 长有效期 | 7天 | ❌ 低（被盗后长期有效） | ✅ 用户无感知 |

**双 Token 的平衡之道：**

| Token 类型 | 有效期 | 用途 | 存储位置 |
|-----------|--------|------|---------|
| **Access Token** | 15分钟 | 访问受保护资源 | 前端内存/localStorage |
| **Refresh Token** | 7天 | 换取新的 Access Token | 前端安全存储 + 服务端数据库 |

这样设计的好处：
1. Access Token 有效期短，即使泄露危害有限
2. Refresh Token 有效期长，但只用于刷新（不携带业务权限），且服务端可以主动撤销
3. 用户体验好——Access Token 过期时，前端自动用 Refresh Token 换新的，用户无感知

### 2.3 Token 流转时序

下面用文字描述整个认证流程的时序：

```
┌──────────┐                          ┌──────────┐                    ┌──────────┐
│  客户端   │                          │   服务端  │                    │  数据库   │
└────┬─────┘                          └────┬─────┘                    └────┬─────┘
     │                                      │                               │
     │  1. POST /auth/login                 │                               │
     │  {username, password}                │                               │
     │─────────────────────────────────────>│                               │
     │                                      │  2. 验证用户名密码              │
     │                                      │──────────────────────────────>│
     │                                      │  3. 返回用户信息               │
     │                                      │<──────────────────────────────│
     │                                      │                               │
     │                                      │  4. 生成 AccessToken           │
     │                                      │     生成 RefreshToken          │
     │                                      │  5. 存储 RefreshToken 哈希     │
     │                                      │──────────────────────────────>│
     │  6. 返回双Token                      │                               │
     │<─────────────────────────────────────│                               │
     │                                      │                               │
     │  7. GET /api/resource               │                               │
     │  Header: Bearer <AccessToken>        │                               │
     │─────────────────────────────────────>│                               │
     │                                      │  8. 验证 AccessToken 签名+过期 │
     │  9. 返回资源数据                      │                               │
     │<─────────────────────────────────────│                               │
     │                                      │                               │
     │  ... AccessToken 过期后 ...           │                               │
     │                                      │                               │
     │  10. POST /auth/refresh              │                               │
     │  {refreshToken}                      │                               │
     │─────────────────────────────────────>│                               │
     │                                      │  11. 验证 RefreshToken         │
     │                                      │  12. 查询数据库确认有效         │
     │                                      │──────────────────────────────>│
     │                                      │  13. 删除旧 RefreshToken       │
     │                                      │  14. 生成新的双Token           │
     │                                      │  15. 存储新 RefreshToken       │
     │                                      │──────────────────────────────>│
     │  16. 返回新的双Token                  │                               │
     │<─────────────────────────────────────│                               │
     │                                      │                               │
```

> **关键设计**：每次刷新都会生成全新的 Refresh Token（Rotation 策略），旧的立即失效。如果攻击者盗取了旧的 Refresh Token，尝试使用时会发现已被删除，从而被检测到异常。

---

## 3. 实战：引入依赖与配置

### 3.1 添加 JJWT / Spring Security 依赖

在 `pom.xml` 中添加以下依赖：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>org.example</groupId>
    <artifactId>jwt-java-eight</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <properties>
        <java.version>1.8</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring-boot.version>2.7.6</spring-boot.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- ========== JWT 核心依赖 ========== -->
        <!-- JJWT 分为三个模块：API、实现、JSON序列化 -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.11.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>  <!-- 运行时依赖，编译时只需要 api 模块 -->
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>

        <!-- ========== Spring Boot 核心 ========== -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- ========== 数据库 ========== -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>2.2.2</version>
        </dependency>

        <!-- ========== 工具库 ========== -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>5.8.22</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- ========== API 文档 ========== -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-ui</artifactId>
            <version>1.6.15</version>
        </dependency>

        <!-- ========== 测试 ========== -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

**为什么 JJWT 要拆成三个包？**

这是 JJWT 的设计哲学——**面向接口编程**。你的业务代码只依赖 `jjwt-api`（接口），具体的实现（`jjwt-impl`）和 JSON 处理（`jjwt-jackson`）在运行时自动加载。这样如果你要换 JSON 库（比如从 Jackson 换成 Gson），只需要替换 `jjwt-jackson` 为 `jjwt-gson`，业务代码一行不改。

### 3.2 application.yml 配置详解

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/jwt_java_eight?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: org.example.jwtjavaeight.domain.entity
  configuration:
    map-underscore-to-camel-case: true   # 下划线转驼峰，user_id → userId

# ========== JWT 自定义配置 ==========
jwt:
  secret: YourSecretKeyHereMustBeAtLeast256BitsLongForHS256Algorithm
  access-token-expiration: 900000       # 15分钟 = 15 * 60 * 1000
  refresh-token-expiration: 604800000   # 7天 = 7 * 24 * 60 * 60 * 1000
  token-prefix: "Bearer "               # 注意末尾有一个空格！
  header: Authorization
```

**配置项解读：**

| 配置项 | 值 | 说明 |
|--------|-----|------|
| `jwt.secret` | 至少32字符 | HS256 要求密钥至少 256 bits（32字节） |
| `jwt.access-token-expiration` | 900000ms | Access Token 15分钟过期，强制用户频繁刷新 |
| `jwt.refresh-token-expiration` | 604800000ms | Refresh Token 7天过期，周期性要求重新登录 |
| `jwt.token-prefix` | "Bearer " | OAuth 2.0 标准前缀，末尾有空格 |
| `jwt.header` | Authorization | HTTP 标准认证头 |

> **⚠️ 生产环境注意**：`secret` 不应该硬编码在配置文件中。应该使用环境变量 `${JWT_SECRET}` 或配置中心（Nacos/Apollo）管理。

---

## 4. 核心模块开发

### 4.1 实体类与 DTO

#### 4.1.1 数据库表设计

首先创建数据库表，执行 `docs/sql/init_jwt.sql`：

```sql
CREATE DATABASE IF NOT EXISTS jwt_java_eight
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE jwt_java_eight;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(64)  NOT NULL COMMENT '用户名',
    password    VARCHAR(128) NOT NULL COMMENT '密码(BCrypt)',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1-正常 0-禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 刷新令牌表
CREATE TABLE IF NOT EXISTS sys_refresh_token (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    token_hash  VARCHAR(64)  NOT NULL COMMENT 'Refresh Token的SHA256哈希',
    expire_time DATETIME     NOT NULL COMMENT '过期时间',
    device_info VARCHAR(255) DEFAULT NULL COMMENT '设备信息',
    jti_id      VARCHAR(64)  NOT NULL COMMENT 'JWT唯一标识',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_jti (jti_id),
    KEY idx_expire (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='刷新令牌表';
```

> **设计决策**：为什么数据库存的是 `token_hash` 而不是 Token 原文？
>
> 因为 Refresh Token 是高价值凭证，类似于密码。如果数据库被拖库，攻击者不应该直接获得可用的 Token。我们存储 SHA256 哈希，验证时对比哈希值即可。

#### 4.1.2 实体类

```java
package org.example.jwtjavaeight.domain.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统用户实体
 * 对应数据库表 sys_user
 */
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String password;
    private Integer status;     // 1-正常 0-禁用
    private Date createTime;

    // getter/setter 省略...
}
```

```java
package org.example.jwtjavaeight.domain.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * 刷新令牌实体
 * 对应数据库表 sys_refresh_token
 */
public class SysRefreshToken implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String tokenHash;   // SHA256(refreshToken)
    private Date expireTime;
    private String deviceInfo;  // 预留：多设备管理
    private String jtiId;       // JWT ID，用于精确删除
    private Date createTime;

    // getter/setter 省略...
}
```

#### 4.1.3 DTO（Data Transfer Object）

DTO 用于接口数据传输，与实体类解耦：

```java
package org.example.jwtjavaeight.domain.dto;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求 DTO
 * 使用 JSR-303 注解进行参数校验
 */
public class LoginRequest implements Serializable {

    @NotBlank(message = "用户名不能为空")    // 非空校验
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    // getter/setter...
}
```

```java
package org.example.jwtjavaeight.domain.dto;

/**
 * 登录/刷新响应 DTO
 * 返回双 Token 给客户端
 */
public class LoginResponse implements Serializable {

    private String accessToken;     // 访问令牌
    private String refreshToken;    // 刷新令牌
    private String tokenType;       // "Bearer"

    public LoginResponse(String accessToken, String refreshToken, String tokenType) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
    }
    // getter/setter...
}
```

> **为什么要有 DTO？**
>
> 实体类直接暴露给前端有两个风险：
> 1. 安全问题——密码等敏感字段可能泄露
> 2. 耦合问题——数据库字段变动会直接影响接口契约
>
> DTO 是接口和业务逻辑之间的"隔离层"。

### 4.2 JWT 工具类（JwtUtil）

这是整个项目的核心工具，负责 Token 的生成与解析。

```java
package org.example.jwtjavaeight.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.example.jwtjavaeight.config.JwtConfig;
import org.example.jwtjavaeight.constants.SecurityConstants;
import org.springframework.stereotype.Component;

/**
 * JWT 工具类
 *
 * 职责：
 * 1. 生成 Access Token（含用户信息和权限）
 * 2. 生成 Refresh Token（仅含用户ID）
 * 3. 解析 Token 获取 Claims
 * 4. 验证 Token 是否有效
 */
@Component
public class JwtUtil {

    private final JwtConfig jwtConfig;

    public JwtUtil(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    /**
     * 生成 Access Token
     *
     * @param userId      用户ID
     * @param username    用户名（作为 subject）
     * @param authorities 权限列表（逗号分隔）
     * @return 签名后的 JWT 字符串
     */
    public String generateAccessToken(Long userId, String username, String authorities) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getAccessTokenExpiration());
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setId(jti)                                          // jti: 唯一标识
                .setSubject(username)                                // sub: 主题
                .claim(SecurityConstants.CLAIM_USER_ID, userId)      // 自定义: 用户ID
                .claim(SecurityConstants.CLAIM_AUTHORITIES, authorities) // 自定义: 权限
                .setIssuedAt(now)                                    // iat: 签发时间
                .setExpiration(expiration)                           // exp: 过期时间
                // Keys.hmacShaKeyFor() 将字节数组转为 SecretKey，确保密钥长度满足算法要求
                .signWith(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 生成 Refresh Token
     * 注意：Refresh Token 不包含权限信息，只包含用户ID
     * 这是一个安全设计——即使 Refresh Token 泄露，攻击者也无法直接获取用户权限
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getRefreshTokenExpiration());
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setId(jti)
                .claim(SecurityConstants.CLAIM_USER_ID, userId)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 Token，获取 Claims（声明集合）
     * 如果 Token 无效或已过期，会抛出异常
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtConfig.getSecret().getBytes())  // 设置验签密钥
                .build()
                .parseClaimsJws(token)                           // 解析并验证签名
                .getBody();                                      // 获取 Payload
    }

    /**
     * 验证 Token 是否有效
     * 内部会检查：签名是否正确、是否过期
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            // 签名错误、过期、格式错误等都会抛异常
            return false;
        }
    }

    /** 从 Token 中提取 JTI（JWT ID） */
    public String getJtiFromToken(String token) {
        return parseToken(token).getId();
    }

    /** 从 Token 中提取用户ID */
    public Long getUserIdFromToken(String token) {
        return parseToken(token).get(SecurityConstants.CLAIM_USER_ID, Long.class);
    }
}
```

**穿插知识点：Java 8 Optional 在防止空指针中的应用**

在实际项目中，解析 Token 的 Claims 可能返回 null。我们可以用 `Optional` 来优雅地处理：

```java
import java.util.Optional;

/**
 * 安全地从 Token 中提取用户ID
 * 使用 Java 8 Optional 避免 NPE
 */
public Optional<Long> getUserIdSafely(String token) {
    try {
        Claims claims = parseToken(token);
        // Optional.ofNullable 包装可能为 null 的值
        return Optional.ofNullable(claims.get(SecurityConstants.CLAIM_USER_ID, Long.class));
    } catch (Exception e) {
        // Token 无效时返回空的 Optional
        return Optional.empty();
    }
}

// 使用示例：
jwtUtil.getUserIdSafely(token)
       .ifPresent(userId -> {
           // 只有 userId 存在时才执行
           log.info("当前用户ID: {}", userId);
       });

// 或者提供默认值：
Long userId = jwtUtil.getUserIdSafely(token).orElse(-1L);
```

> **Java 8 知识点**：`Optional` 是 Java 8 引入的容器类，用于表示"值可能不存在"。它迫使开发者显式处理 null 的情况，而不是忘记检查然后在运行时抛出 NullPointerException。

### 4.3 用户认证过滤器（JwtAuthenticationFilter）

这个过滤器是 JWT 认证的核心——它拦截每个请求，提取并验证 Token，然后把用户信息放入 Spring Security 的上下文中。

```java
package org.example.jwtjavaeight.security;

import cn.hutool.json.JSONUtil;
import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.example.jwtjavaeight.common.Result;
import org.example.jwtjavaeight.common.ResultCode;
import org.example.jwtjavaeight.config.JwtConfig;
import org.example.jwtjavaeight.constants.SecurityConstants;
import org.example.jwtjavaeight.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 认证过滤器
 *
 * 继承 OncePerRequestFilter 确保每个请求只经过一次此过滤器。
 * 职责：
 * 1. 从 Authorization Header 中提取 Token
 * 2. 验证 Token 有效性
 * 3. 将用户信息注入 Spring Security 上下文
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final JwtConfig jwtConfig;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, JwtConfig jwtConfig) {
        this.jwtUtil = jwtUtil;
        this.jwtConfig = jwtConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 获取 Authorization 头
        String header = request.getHeader(jwtConfig.getHeader());

        // 2. 如果没有 Token 或格式不对，直接放行（交给 Spring Security 决定是否拒绝）
        if (!StringUtils.hasText(header) || !header.startsWith(jwtConfig.getTokenPrefix())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 截取 "Bearer " 之后的 Token 字符串
        String token = header.substring(jwtConfig.getTokenPrefix().length());

        try {
            // 4. 验证 Token
            if (!jwtUtil.validateToken(token)) {
                log.warn("[JWT Filter] Token验证失败");
                writeUnauthorized(response);
                return;
            }

            // 5. 解析 Token 中的用户信息
            Claims claims = jwtUtil.parseToken(token);
            Long userId = claims.get(SecurityConstants.CLAIM_USER_ID, Long.class);
            String username = claims.getSubject();
            String authoritiesStr = claims.get(SecurityConstants.CLAIM_AUTHORITIES, String.class);

            // 6.【Java 8 Stream】将逗号分隔的权限字符串转为权限对象列表
            List<SimpleGrantedAuthority> authorities = Stream.of(authoritiesStr.split(","))
                    .map(SimpleGrantedAuthority::new)   // 方法引用：等价于 s -> new SimpleGrantedAuthority(s)
                    .collect(Collectors.toList());

            // 7. 构建 Spring Security 认证对象并设置到上下文
            JwtUserDetails userDetails = new JwtUserDetails(buildUserEntity(userId, username));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("[JWT Filter] 用户 {} 认证成功", username);

        } catch (Exception e) {
            log.error("[JWT Filter] Token解析异常: {}", e.getMessage());
            writeUnauthorized(response);
            return;
        }

        // 8. 放行请求，继续执行后续过滤器和 Controller
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(
                JSONUtil.toJsonStr(Result.failure(ResultCode.UNAUTHORIZED.getCode(), "Token已过期或无效")));
    }

    private SysUser buildUserEntity(Long userId, String username) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername(username);
        user.setStatus(1);
        return user;
    }
}
```

**穿插知识点：Java 8 Stream 在权限转换中的应用**

上面代码中第 6 步使用了 Stream API，让我详细解释：

```java
// 假设 authoritiesStr = "ROLE_USER,ROLE_ADMIN"

// ===== 传统 Java 写法 =====
String[] parts = authoritiesStr.split(",");
List<SimpleGrantedAuthority> authorities = new ArrayList<>();
for (String part : parts) {
    authorities.add(new SimpleGrantedAuthority(part));
}

// ===== Java 8 Stream 写法 =====
List<SimpleGrantedAuthority> authorities = Stream.of(authoritiesStr.split(","))
        .map(SimpleGrantedAuthority::new)      // 转换：String → SimpleGrantedAuthority
        .collect(Collectors.toList());         // 收集为 List

// ===== 如果需要过滤空字符串 =====
List<SimpleGrantedAuthority> authorities = Stream.of(authoritiesStr.split(","))
        .filter(s -> !s.isEmpty())             // 过滤掉空字符串
        .map(String::trim)                     // 去除首尾空格
        .map(SimpleGrantedAuthority::new)      // 转换类型
        .collect(Collectors.toList());         // 收集结果
```

> **Stream 三板斧**：`filter`（过滤）→ `map`（转换）→ `collect`（收集）。这个模式在 Java 8 开发中无处不在，务必熟练掌握。

### 4.4 认证失败处理器

当未认证用户访问受保护资源，或认证失败时，Spring Security 会调用这个处理器：

```java
package org.example.jwtjavaeight.config;

import cn.hutool.json.JSONUtil;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.example.jwtjavaeight.common.Result;
import org.example.jwtjavaeight.common.ResultCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 安全异常统一处理器
 *
 * 实现两个接口：
 * - AuthenticationEntryPoint：处理未认证（401）
 * - AccessDeniedHandler：处理无权限（403）
 *
 * 为什么需要这个类？
 * 默认情况下 Spring Security 会返回 HTML 页面或重定向到登录页。
 * 在前后端分离架构中，我们需要统一返回 JSON 格式的错误信息。
 */
@Component
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    /** 处理 401：未认证 */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(
                JSONUtil.toJsonStr(Result.failure(
                        ResultCode.UNAUTHORIZED.getCode(),
                        ResultCode.UNAUTHORIZED.getMessage())));
    }

    /** 处理 403：无权限 */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.getWriter().write(
                JSONUtil.toJsonStr(Result.failure(
                        ResultCode.FORBIDDEN.getCode(),
                        ResultCode.FORBIDDEN.getMessage())));
    }
}
```

### 4.5 全局异常处理器

使用 `@RestControllerAdvice` 统一处理所有 Controller 抛出的异常：

```java
package org.example.jwtjavaeight.exception;

import org.example.jwtjavaeight.common.Result;
import org.example.jwtjavaeight.common.ResultCode;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * 好处：
 * 1. Controller 代码干净——不需要 try-catch
 * 2. 统一错误响应格式
 * 3. 集中管理日志记录
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常（我们自定义的异常基类） */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.failure(e.getCode(), e.getMessage());
    }

    /** Spring Security 密码错误异常 */
    @ExceptionHandler(BadCredentialsException.class)
    public Result<Void> handleBadCredentialsException(BadCredentialsException e) {
        return Result.failure(ResultCode.BAD_CREDENTIALS.getCode(),
                            ResultCode.BAD_CREDENTIALS.getMessage());
    }

    /** 参数校验异常（@Valid 校验不通过时抛出） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return Result.failure(400, message);
    }

    /** 兜底：所有未处理的异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        return Result.failure(ResultCode.ERROR.getCode(), "系统异常，请联系管理员");
    }
}
```

> **设计模式**：这里运用了"统一异常处理"模式。Service 层只需要 `throw new XxxException()`，Controller 层完全不需要关心异常处理，全部由 `GlobalExceptionHandler` 兜底。这让代码非常干净。

---

## 5. Spring Security 配置整合

### 5.1 配置 SecurityFilterChain

Spring Security 的核心配置类，决定了整个安全策略。本项目采用 **Filter 链标准流程** 处理登录认证（已完成）：

```java
package org.example.jwtjavaeight.config;

import java.util.Arrays;
import java.util.Collections;
import org.example.jwtjavaeight.security.JwtAuthenticationFilter;
import org.example.jwtjavaeight.security.JwtAuthenticationProvider;
import org.example.jwtjavaeight.security.JwtLoginFilter;
import org.example.jwtjavaeight.security.handler.LoginFailureHandler;
import org.example.jwtjavaeight.security.handler.LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityExceptionHandler securityExceptionHandler;
    private final UserDetailsService userDetailsService;
    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;
    private final AuthenticationConfiguration authenticationConfiguration;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          SecurityExceptionHandler securityExceptionHandler,
                          UserDetailsService userDetailsService,
                          LoginSuccessHandler loginSuccessHandler,
                          LoginFailureHandler loginFailureHandler,
                          AuthenticationConfiguration authenticationConfiguration) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.securityExceptionHandler = securityExceptionHandler;
        this.userDetailsService = userDetailsService;
        this.loginSuccessHandler = loginSuccessHandler;
        this.loginFailureHandler = loginFailureHandler;
        this.authenticationConfiguration = authenticationConfiguration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 通过 AuthenticationConfiguration 获取 AuthenticationManager，避免循环依赖
        AuthenticationManager authenticationManager = authenticationConfiguration.getAuthenticationManager();
        http
            // 1. 禁用 CSRF（JWT 自身防CSRF，不需要 CSRF Token）
            .csrf(AbstractHttpConfigurer::disable)
            // 2. 配置 CORS 跨域
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 3. 禁用 Session（JWT 是无状态的，不需要 Session）
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 4. 配置请求授权规则
            .authorizeRequests(authz -> authz
                    .antMatchers("/auth/**").permitAll()
                    .antMatchers("/swagger-ui.html", "/swagger-ui/**",
                               "/swagger-resources/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                    .anyRequest().authenticated())
            // 5. 配置认证失败处理器
            .exceptionHandling(exception -> exception
                    .authenticationEntryPoint(securityExceptionHandler)
                    .accessDeniedHandler(securityExceptionHandler))
            // 6. 注册自定义认证提供者
            .authenticationProvider(new JwtAuthenticationProvider(userDetailsService, passwordEncoder()))
            // 7. 登录拦截 Filter（拦截 POST /auth/login，由框架自动调用认证流程）
            .addFilterBefore(
                    new JwtLoginFilter(authenticationManager, loginSuccessHandler, loginFailureHandler),
                    UsernamePasswordAuthenticationFilter.class)
            // 8. 请求鉴权 Filter（解析已有 Token，设置 SecurityContext）
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Collections.singletonList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**核心设计说明：**

| 设计点 | 实现方式 | 作用 |
|------|---------|------|
| 注入 `AuthenticationConfiguration` | 构造器注入 | 避免 Spring Security 循环依赖，在方法内获取 `AuthenticationManager` |
| 注册 `JwtAuthenticationProvider` | `.authenticationProvider(...)` | 自定义 Provider，通过 `supports()` 判断只处理 `JwtAuthToken` 类型 |
| 添加 `JwtLoginFilter` | `.addFilterBefore(...)` | 继承 `UsernamePasswordAuthenticationFilter`，拦截 `POST /auth/login` |
| 添加 `JwtAuthenticationFilter` | `.addFilterBefore(...)` | 鉴权过滤器，解析 Token 并设置 SecurityContext |
| 无 `@Bean authenticationManager()` | — | 不再需要单独声明，从 `AuthenticationConfiguration` 获取 |

**逐行解读：**

| 配置项 | 作用 | 为什么 |
|--------|------|--------|
| `csrf.disable()` | 禁用 CSRF 保护 | JWT 自带防伪造机制，CSRF Token 多余 |
| `SessionCreationPolicy.STATELESS` | 不创建 Session | JWT 是无状态认证，Session 无意义 |
| `antMatchers("/auth/**").permitAll()` | 白名单放行 | 登录/注册/刷新接口不需要认证 |
| `anyRequest().authenticated()` | 其他接口需认证 | 默认安全策略 |
| `authenticationProvider(...)` | 注册自定义 Provider | 登录认证由 JwtAuthenticationProvider 处理 |
| `addFilterBefore(JwtLoginFilter)` | 插入登录 Filter | 拦截 POST /auth/login，自动触发认证流程 |
| `addFilterBefore(JwtAuthenticationFilter)` | 插入鉴权 Filter | 解析已有 Token，设置 SecurityContext |

### 5.2 密码加密器（BCrypt）

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**为什么用 BCrypt 而不是 MD5/SHA？**

| 特性 | MD5/SHA | BCrypt |
|------|---------|--------|
| 速度 | 极快（这是缺点！） | 刻意设计得慢 |
| 彩虹表 | 容易被彩虹表攻击 | 内置随机盐值 |
| 暴力破解 | 每秒可尝试数十亿次 | 每秒只能尝试数千次 |
| 自适应 | 不可调节 | 可调节计算强度 |

BCrypt 每次加密的结果都不同（因为盐值随机），但验证总是正确的：

```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

// 加密（每次结果不同）
String hash1 = encoder.encode("123456");  // $2a$10$abc...
String hash2 = encoder.encode("123456");  // $2a$10$xyz...

// 验证（总是正确）
encoder.matches("123456", hash1);  // true
encoder.matches("123456", hash2);  // true
encoder.matches("654321", hash1);  // false
```

### 5.3 放行白名单配置

白名单路径不需要携带 Token 即可访问：

```java
.antMatchers("/auth/**").permitAll()
```

这意味着以下接口无需认证：
- `POST /auth/register` — 用户注册
- `POST /auth/login` — 用户登录
- `POST /auth/refresh` — 刷新 Token

> **注意**：`/auth/**` 的 `permitAll()` 只是告诉 Spring Security 不强制要求认证，但并不意味着所有接口都不需要用户信息。`/auth/logout` 在 Controller 中通过 `@AuthenticationPrincipal` 获取当前用户信息——如果请求未携带 Token，`@AuthenticationPrincipal` 将返回 null，Controller 层应自行处理这种情况（如返回"未登录"错误）。因此客户端调用 logout 时仍需携带有效的 Access Token。

### 5.4 登录 Filter 链组件

改造后，登录认证不再由 Controller 手动驱动，而是由 Spring Security Filter 链自动处理。以下是新增的 5 个组件：

#### 5.4.1 JwtAuthToken — 自定义认证令牌

```java
package org.example.jwtjavaeight.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;

public class JwtAuthToken extends UsernamePasswordAuthenticationToken {

    public JwtAuthToken(Object principal, Object credentials) {
        super(principal, credentials);
    }

    public JwtAuthToken(Object principal, Object credentials,
                        Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
    }
}
```

**为什么需要自定义 Token？** `JwtAuthenticationProvider` 通过 `supports(Class<?>)` 方法判断是否处理某种类型的 Token。使用自定义类型可以与 Spring Security 原生的 `UsernamePasswordAuthenticationToken` 隔离，避免冲突。

#### 5.4.2 JwtLoginFilter — 登录拦截过滤器

```java
package org.example.jwtjavaeight.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.jwtjavaeight.domain.dto.LoginRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JwtLoginFilter extends UsernamePasswordAuthenticationFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtLoginFilter(AuthenticationManager authenticationManager,
                          AuthenticationSuccessHandler successHandler,
                          AuthenticationFailureHandler failureHandler) {
        setAuthenticationManager(authenticationManager);
        setAuthenticationSuccessHandler(successHandler);
        setAuthenticationFailureHandler(failureHandler);
        setRequiresAuthenticationRequestMatcher(
                new AntPathRequestMatcher("/auth/login", "POST"));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response)
            throws AuthenticationException {
        try {
            LoginRequest loginReq = objectMapper.readValue(
                    request.getInputStream(), LoginRequest.class);
            String username = loginReq.getUsername() == null ? "" : loginReq.getUsername().trim();
            String password = loginReq.getPassword() == null ? "" : loginReq.getPassword();
            JwtAuthToken authRequest = new JwtAuthToken(username, password);
            setDetails(request, authRequest);
            return this.getAuthenticationManager().authenticate(authRequest);
        } catch (IOException e) {
            throw new RuntimeException("登录请求解析失败", e);
        }
    }
}
```

**工作原理：**
- `setRequiresAuthenticationRequestMatcher` 绑定拦截路径为 `POST /auth/login`
- `attemptAuthentication` 从 request body 读取 JSON，构建 `JwtAuthToken`，交给 `AuthenticationManager`
- 认证成功 → 框架自动调用 `LoginSuccessHandler`
- 认证失败 → 框架自动调用 `LoginFailureHandler`

#### 5.4.3 JwtAuthenticationProvider — 自定义认证提供者

```java
package org.example.jwtjavaeight.security;

import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

public class JwtAuthenticationProvider extends DaoAuthenticationProvider {

    public JwtAuthenticationProvider(UserDetailsService userDetailsService,
                                     PasswordEncoder passwordEncoder) {
        setHideUserNotFoundExceptions(false);
        setUserDetailsService(userDetailsService);
        setPasswordEncoder(passwordEncoder);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAuthToken.class.isAssignableFrom(authentication);
    }
}
```

**关键点：**
- 继承 `DaoAuthenticationProvider`，复用已有的 `UserDetailsServiceImpl`（加载用户 + 校验状态）和 `BCryptPasswordEncoder`（比对密码）
- `setHideUserNotFoundExceptions(false)` 让用户不存在时也抛出明确异常，而非统一返回"用户名或密码错误"
- `supports()` 只处理 `JwtAuthToken` 类型

#### 5.4.4 LoginSuccessHandler — 认证成功处理器

```java
package org.example.jwtjavaeight.security.handler;

import cn.hutool.json.JSONUtil;
import org.example.jwtjavaeight.common.Result;
import org.example.jwtjavaeight.config.JwtConfig;
import org.example.jwtjavaeight.domain.dto.LoginResponse;
import org.example.jwtjavaeight.domain.entity.SysRefreshToken;
import org.example.jwtjavaeight.mapper.RefreshTokenMapper;
import org.example.jwtjavaeight.security.JwtUserDetails;
import org.example.jwtjavaeight.utils.HashUtil;
import org.example.jwtjavaeight.utils.JwtUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final JwtConfig jwtConfig;
    private final RefreshTokenMapper refreshTokenMapper;

    public LoginSuccessHandler(JwtUtil jwtUtil, JwtConfig jwtConfig,
                               RefreshTokenMapper refreshTokenMapper) {
        this.jwtUtil = jwtUtil;
        this.jwtConfig = jwtConfig;
        this.refreshTokenMapper = refreshTokenMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        JwtUserDetails userDetails = (JwtUserDetails) authentication.getPrincipal();
        String authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        String accessToken = jwtUtil.generateAccessToken(
                userDetails.getUserId(), userDetails.getUsername(), authorities);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails.getUserId());

        String tokenHash = HashUtil.sha256(refreshToken);
        String jti = jwtUtil.getJtiFromToken(refreshToken);
        Date expireTime = jwtUtil.parseToken(refreshToken).getExpiration();
        SysRefreshToken sysRefreshToken = new SysRefreshToken();
        sysRefreshToken.setUserId(userDetails.getUserId());
        sysRefreshToken.setTokenHash(tokenHash);
        sysRefreshToken.setExpireTime(expireTime);
        sysRefreshToken.setJtiId(jti);
        refreshTokenMapper.insert(sysRefreshToken);

        LoginResponse loginResponse = new LoginResponse(
                accessToken, refreshToken, jwtConfig.getTokenPrefix().trim());

        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSONUtil.toJsonStr(Result.success(loginResponse)));
    }
}
```

这个 Handler 从 `AuthServiceImpl.login()` 中迁移了 Token 生成和存储逻辑。

#### 5.4.5 LoginFailureHandler — 认证失败处理器

```java
package org.example.jwtjavaeight.security.handler;

import cn.hutool.json.JSONUtil;
import org.example.jwtjavaeight.common.Result;
import org.example.jwtjavaeight.common.ResultCode;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(
                JSONUtil.toJsonStr(Result.failure(
                        ResultCode.BAD_CREDENTIALS.getCode(),
                        exception.getMessage())));
    }
}
```

#### 5.4.6 项目当前的登录流程（Filter 链标准流程）

```
POST /auth/login { "username": "admin", "password": "123456" }
    │
    ▼
Spring Security Filter Chain 开始执行
    │
    ▼
JwtAuthenticationFilter (OncePerRequestFilter)
    ├── 检查 Authorization Header
    ├── 无 Token 或不是 /auth/login → 直接放行
    └── filterChain.doFilter() → 继续下一个 Filter
    │
    ▼
JwtLoginFilter (UsernamePasswordAuthenticationFilter)
    ├── 通过 AntPathRequestMatcher 匹配 "POST /auth/login"
    ├── 拦截成功 → attemptAuthentication()
    │     ├── 从 request body 读取 JSON (ObjectMapper)
    │     ├── 提取 username, password
    │     ├── 构建 JwtAuthToken(username, password)
    │     └── authenticationManager.authenticate(authRequest)
    │           │
    │           ▼
    │       JwtAuthenticationProvider.authenticate()
    │           ├── supports(JwtAuthToken.class) → true
    │           ├── UserDetailsServiceImpl.loadUserByUsername()
    │           │     → UserMapper.findByUsername() (MySQL)
    │           │     → 检查 status 字段
    │           │     → 构建 JwtUserDetails (含权限)
    │           ├── BCryptPasswordEncoder.matches(rawPassword, encodedPassword)
    │           └── 返回 Authentication (principal=JwtUserDetails, authenticated=true)
    │
    ├── 认证成功 → LoginSuccessHandler.onAuthenticationSuccess()
    │     ├── 从 Authentication 提取 JwtUserDetails
    │     ├── 收集权限列表 (Stream API: map + collect)
    │     ├── jwtUtil.generateAccessToken(userId, username, authorities)
    │     ├── jwtUtil.generateRefreshToken(userId)
    │     ├── SHA256 哈希 refreshToken → tokenHash
    │     ├── refreshTokenMapper.insert(tokenHash, userId, expireTime, jti)
    │     └── response.write(Result.success(LoginResponse))
    │
    └── 认证失败 → LoginFailureHandler.onAuthenticationFailure()
          ├── response.setStatus(401)
          └── response.write(Result.failure(exception.getMessage()))
```

**关键技术点：**

1. **Filter 链自动触发**：`JwtLoginFilter` 通过 `setRequiresAuthenticationRequestMatcher` 绑定 `POST /auth/login`，Spring Security 自动在匹配时调用 `attemptAuthentication()`

2. **无需 Controller**：`AuthController` 中没有 `login()` 方法，登录完全由 Filter 层处理

3. **责任链模式**：认证成功/失败由专门的 Handler 处理，职责单一，易于扩展

4. **与鉴权分离**：`JwtAuthenticationFilter` 负责解析已有 Token（鉴权），`JwtLoginFilter` 负责处理登录认证，互不干扰

> **架构优势**：相比传统的 Controller 手动驱动方式，Filter 链标准流程将认证逻辑从业务层剥离到安全层，Controller 完全无需关心认证细节，符合单一职责原则和关注点分离原则。

---

## 6. 双 Token 刷新机制实现

### 6.1 Refresh Token 的存储策略

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| **数据库** | 简单可靠、易于管理 | 每次刷新有 I/O 开销 | 中小型项目（本项目） |
| **Redis** | 速度快、天然支持 TTL | 需要额外组件 | 高并发项目 |
| **内存** | 最快 | 重启丢失、不支持分布式 | 仅限开发测试 |

本项目采用**数据库方案**，存储 Refresh Token 的 SHA256 哈希值。

### 6.2 刷新接口的设计与实现

> **架构说明**：本项目采用 Filter 链标准流程，登录逻辑由 `JwtLoginFilter` 拦截并由 `LoginSuccessHandler` 处理。`AuthServiceImpl` 只负责注册、刷新、登出三个业务功能。

**实际代码（项目当前状态）：**

```java
package org.example.jwtjavaeight.service.impl;

/**
 * 认证服务实现类
 * 
 * 核心业务：
 * - register：用户注册
 * - refresh：刷新 Token（Token Rotation 策略）
 * - logout：清除 Refresh Token
 * 
 * 注意：登录逻辑由 JwtLoginFilter + LoginSuccessHandler 处理，不在此类中
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;
    private final JwtConfig jwtConfig;
    private final RefreshTokenMapper refreshTokenMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(JwtUtil jwtUtil,
                           JwtConfig jwtConfig,
                           RefreshTokenMapper refreshTokenMapper,
                           UserMapper userMapper,
                           PasswordEncoder passwordEncoder) {
        this.jwtUtil = jwtUtil;
        this.jwtConfig = jwtConfig;
        this.refreshTokenMapper = refreshTokenMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void register(RegisterRequest registerRequest) {
        SysUser existingUser = userMapper.findByUsername(registerRequest.getUsername());
        if (existingUser != null) {
            throw new UsernameExistsException();
        }

        SysUser user = new SysUser();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setStatus(1);
        user.setCreateTime(new Date());
        userMapper.insert(user);
    }

    /**
     * 刷新 Token
     *
     * 安全设计要点：
     * 1. 验证 Refresh Token 的签名和过期时间
     * 2. 在数据库中确认该 Token 确实存在（防止复用已撤销的 Token）
     * 3. 删除旧 Token 记录（Token Rotation）
     * 4. 从数据库加载最新用户信息（确认用户未被禁用）
     * 5. 生成全新的双 Token
     */
    @Override
    @Transactional
    public LoginResponse refresh(RefreshRequest refreshRequest) {
        String refreshTokenValue = refreshRequest.getRefreshToken();

        // 第一关：JWT 本身的验证（签名 + 过期时间）
        if (!jwtUtil.validateToken(refreshTokenValue)) {
            throw new TokenExpiredException("Refresh Token无效或已过期");
        }

        Claims claims = jwtUtil.parseToken(refreshTokenValue);
        Long userId = claims.get(SecurityConstants.CLAIM_USER_ID, Long.class);
        String jti = claims.getId();

        // 第二关：数据库验证（防止 Token 被撤销后仍然使用）
        String tokenHash = HashUtil.sha256(refreshTokenValue);
        SysRefreshToken storedToken = refreshTokenMapper.findByTokenHash(tokenHash);

        if (storedToken == null) {
            throw new TokenExpiredException("Refresh Token不存在");
        }

        if (storedToken.getExpireTime().before(new Date())) {
            refreshTokenMapper.deleteByJtiId(jti);
            throw new TokenExpiredException("Refresh Token已过期");
        }

        // Token Rotation：删除旧 Token
        refreshTokenMapper.deleteByJtiId(jti);

        // 第三关：验证用户状态
        SysUser user = userMapper.findById(userId);
        if (user == null) {
            throw new TokenExpiredException("用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new TokenExpiredException("用户已禁用");
        }

        // 生成全新的双 Token
        // ⚠️ 简化处理：此处硬编码为 ROLE_USER。生产环境应从数据库/权限表动态加载用户实际权限
        String authorities = "ROLE_USER";
        String newAccessToken = jwtUtil.generateAccessToken(userId, user.getUsername(), authorities);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);

        // 内联存储新 Refresh Token 的哈希值（不再作为独立私有方法）
        String newTokenHash = HashUtil.sha256(newRefreshToken);
        String newJti = jwtUtil.getJtiFromToken(newRefreshToken);
        Date newExpireTime = jwtUtil.parseToken(newRefreshToken).getExpiration();
        SysRefreshToken newSysRefreshToken = new SysRefreshToken();
        newSysRefreshToken.setUserId(userId);
        newSysRefreshToken.setTokenHash(newTokenHash);
        newSysRefreshToken.setExpireTime(newExpireTime);
        newSysRefreshToken.setJtiId(newJti);
        refreshTokenMapper.insert(newSysRefreshToken);

        return new LoginResponse(newAccessToken, newRefreshToken, jwtConfig.getTokenPrefix().trim());
    }

    /**
     * 登出
     * 删除该用户的所有 Refresh Token 记录
     */
    @Override
    @Transactional
    public void logout(Long userId) {
        refreshTokenMapper.deleteByUserId(userId);
    }
}
```

### 6.3 穿插知识点：使用 Stream API 校验 Token 黑名单

在某些场景下，你可能需要维护一个"已注销但尚未过期的 Access Token 黑名单"。以下是两个用 Stream API 实现的核心方法（演示用，生产环境建议使用 Redis）：

```java
// Token 黑名单（线程安全 Set，存储被注销的 Token JTI）
private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

// 【Stream 演示 1】批量检查多个 Token，返回所有已被拉黑的 JTI
public List<String> findBlacklisted(List<String> jtiList) {
    return jtiList.stream()
            .filter(blacklist::contains)     // 方法引用：等价于 jti -> blacklist.contains(jti)
            .collect(Collectors.toList());
}

// 【Stream 演示 2】清理已过期的黑名单条目
public void cleanExpired(Map<String, Date> jtiExpireMap) {
    Date now = new Date();
    Set<String> expiredJtis = jtiExpireMap.entrySet().stream()
            .filter(entry -> entry.getValue().before(now))  // 过滤：过期时间在当前时间之前
            .map(Map.Entry::getKey)                         // 提取：只要 Key（JTI）
            .collect(Collectors.toSet());                   // 收集为 Set
    blacklist.removeAll(expiredJtis);
}
```

> **Stream 进阶**：`filter` + `map` + `collect` 是最常见的组合。`filter` 像漏斗一样筛选元素，`map` 像工厂一样转换元素，`collect` 把流中的元素收集到容器中。

---

## 7. 接口测试与调试

### 7.1 使用 curl/Postman 获取 Token

启动应用后，Swagger UI 地址：`http://localhost:8080/swagger-ui.html`

#### 步骤一：注册用户

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456"}'
```

**预期响应：**

```json
{
    "code": 200,
    "message": "操作成功",
    "data": null
}
```

#### 步骤二：登录获取 Token

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456"}'
```

**预期响应：**

```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI...",
        "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI...",
        "tokenType": "Bearer"
    }
}
```

### 7.2 携带 Token 访问受保护资源

假设你有一个受保护的接口，需要携带 Token 访问：

```bash
curl -X GET http://localhost:8080/api/user/info \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI..."
```

**如果 Token 有效**，返回正常数据。

**如果 Token 缺失或无效**：

```json
{
    "code": 401,
    "message": "未登录或Token已过期"
}
```

### 7.3 Token 过期场景模拟

为了快速测试过期场景，可以临时修改配置：

```yaml
jwt:
  access-token-expiration: 10000   # 改为 10 秒
```

**测试步骤：**

1. 登录获取 Token
2. 等待 10 秒
3. 用过期的 Access Token 访问接口 → 得到 401
4. 用 Refresh Token 刷新：

```bash
curl -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI..."}'
```

5. 得到新的双 Token，继续使用

### 7.4 登出测试

```bash
curl -X POST http://localhost:8080/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI..."
```

登出后，该用户的所有 Refresh Token 都会被删除。

---

## 8. 总结与面试考点

### 8.1 本案例的不足与改进方向

| 现状 | 改进方向 | 说明 |
|------|---------|------|
| 无 Access Token 撤销机制 | 引入 Redis 黑名单 | 登出后 Access Token 在过期前仍然有效 |
| 权限硬编码 | 引入 RBAC 权限模型 | 添加角色表、权限表 |
| 无接口限流 | 引入 Sentinel/RateLimiter | 防止暴力破解 |
| 单机部署 | 引入 Redis 存储 RefreshToken | 支持分布式部署 |
| 密钥硬编码 | 使用配置中心 | 密钥不应出现在代码或配置文件中 |
| 无审计日志 | 记录关键操作 | 登录成功/失败、Token刷新等 |

### 8.2 JWT 常见面试题

#### Q1：JWT 的 Token 被盗了怎么办？

**答**：
- **Access Token 被盗**：由于有效期短（15分钟），危害有限。可以引入 Token 黑名单机制主动撤销。
- **Refresh Token 被盗**：通过 Token Rotation（每次刷新都换新 Token），当合法用户发现 Token 失效时就知道被盗了。同时数据库端可以直接删除该用户的所有 Refresh Token 记录。

#### Q2：JWT 如何实现"注销"？

**答**：JWT 本身是无状态的，签发后无法"作废"。常见方案：

1. **短过期时间 + Refresh Token 撤销**（本项目方案）
   - 登出时删除 Refresh Token，Access Token 自然过期
   - 缺点：Access Token 在过期前仍有效

2. **Token 黑名单（Redis）**
   - 登出时将 Access Token 的 JTI 加入 Redis 黑名单
   - 每次请求都检查黑名单
   - 缺点：引入了"状态"，部分违背了 JWT 无状态的初衷

3. **密钥轮换**
   - 修改签名密钥使所有已签发的 Token 失效
   - 缺点：所有用户都被踢下线

#### Q3：为什么不把 JWT 存储在 Cookie 中？

**答**：
- Cookie 有 CSRF 攻击风险
- Cookie 有跨域限制
- 移动端不方便使用 Cookie
- JWT 放在 Authorization Header 中是 OAuth 2.0 标准做法

#### Q4：Access Token 和 Refresh Token 可以用同一个密钥吗？

**答**：可以（本项目就是这样），但生产环境建议使用不同密钥，原因：
- 职责分离：即使 Access Token 的密钥泄露，Refresh Token 仍然安全
- 独立轮换：可以单独更换某一个 Token 的密钥

#### Q5：Token 中应该存哪些信息？

**答**：
- ✅ 应该存：用户ID、用户名、角色/权限、过期时间
- ❌ 不应该存：密码、手机号、邮箱等敏感信息
- ❌ 不应该存：大量数据（Token 会变得很长，每次请求都要传输）

#### Q6：Spring Security 的过滤器链是怎么工作的？

**答**：

```
请求 → SecurityFilterChain → JwtAuthenticationFilter → UsernamePasswordAuthenticationFilter → ... → Controller
                                      ↓
                           1. 提取 Token
                           2. 验证签名+过期
                           3. 设置 SecurityContext
                                      ↓
                            后续过滤器可以通过
                            SecurityContextHolder.getContext()
                            获取当前用户信息
```

我们的 `JwtAuthenticationFilter` 通过 `addFilterBefore` 插入到 `UsernamePasswordAuthenticationFilter` 之前。这样当请求携带了有效 Token 时，我们直接设置好认证信息，Spring Security 就不会再要求表单登录了。

---

## 9. 常见报错与排查指南

### 9.1 数据库连接失败

**报错信息：**
```
Communications link failure
The last packet sent successfully to the server was 0 milliseconds ago.
```

**排查步骤：**
1. 确认 MySQL 服务已启动：`systemctl status mysql`（Linux）或在服务管理器中查看（Windows）
2. 检查 `application.yml` 中的数据库地址、端口、用户名和密码是否正确
3. 确认数据库 `jwt_java_eight` 已创建：`CREATE DATABASE jwt_java_eight;`
4. 检查 MySQL 是否允许远程连接（如果数据库不在本机）

### 9.2 BCrypt 密码格式不匹配

**报错信息：**
```
IllegalArgumentException: There is no PasswordEncoder mapped for the id "null"
```
或
```
BadCredentialsException: Bad credentials
```

**原因：** 数据库中的密码不是 BCrypt 格式。BCrypt 加密后的密码以 `$2a$` 或 `$2b$` 开头，长度固定为 60 字符。

**解决：** 注册时确保使用 `BCryptPasswordEncoder.encode()` 加密密码，不要手动插入数据库。

### 9.3 Token 已过期但仍返回 401

**现象：** 用 Refresh Token 刷新后，新的 Access Token 仍然无法使用。

**排查：**
1. 检查是否在 `SecurityConfig` 中正确注册了 `JwtLoginFilter`（在 `JwtAuthenticationFilter` **之前**）
2. 确认前端使用的是刷新后返回的新 Token，而非旧 Token
3. 检查 `JwtUtil.validateToken()` 的异常处理逻辑

### 9.4 启动时报循环依赖错误

**报错信息：**
```
BeanCurrentlyInCreationException: Error creating bean with name 'securityFilterChain'
```

**原因：** `SecurityConfig` 中通过 `@Bean` 公开了 `AuthenticationManager`，同时又在 `securityFilterChain()` 中注入它。

**解决：** 不要声明 `@Bean authenticationManager()`。改为注入 `AuthenticationConfiguration`，在方法内部获取：

```java
@Autowired
private AuthenticationConfiguration authenticationConfiguration;

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    AuthenticationManager authenticationManager = authenticationConfiguration.getAuthenticationManager();
    // ...
}
```

### 9.5 CORS 跨域问题

**报错信息（浏览器控制台）：**
```
Access to XMLHttpRequest at 'http://localhost:8080/auth/login' from origin 'http://localhost:3000'
has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present.
```

**排查：**
1. 确认 `SecurityConfig` 中已配置 `cors()` 和 `CorsConfigurationSource`
2. 前端请求的 `Origin` 是否在允许列表中（本项目默认 `*`，生产环境应限制为具体域名）
3. 如果使用了 Nginx 反向代理，检查 Nginx 的 CORS 配置是否冲突

### 9.6 Swagger UI 无法访问（401）

**现象：** 访问 `http://localhost:8080/swagger-ui.html` 返回 401。

**排查：** 确认 `SecurityConfig` 中 Swagger 路径已加入白名单：

```java
.antMatchers("/swagger-ui.html", "/swagger-ui/**",
           "/swagger-resources/**", "/v3/api-docs/**", "/webjars/**").permitAll()
```

---

## 附录 A：统一响应体设计

```java
package org.example.jwtjavaeight.common;

/**
 * 统一 API 响应包装类
 * 所有接口都返回这个格式，前端可以统一处理
 */
public class Result<T> implements Serializable {

    private Integer code;     // 业务状态码
    private String message;   // 提示信息
    private T data;           // 响应数据

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    public static <T> Result<T> failure(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
```

## 附录 B：错误码清单

| 错误码 | 枚举名 | 含义 | 触发场景 |
|--------|--------|------|---------|
| 200 | SUCCESS | 操作成功 | 正常响应 |
| 400 | BAD_CREDENTIALS | 用户名或密码错误 | 登录失败 |
| 400 | USERNAME_EXISTS | 用户名已存在 | 注册重复 |
| 401 | UNAUTHORIZED | 未登录或Token已过期 | Token验证失败 |
| 403 | FORBIDDEN | 没有权限 | 权限不足 |
| 500 | ERROR | 系统异常 | 未预期的错误 |

## 附录 C：Java 8 特性总结（项目实际应用）

本项目中实际使用的 Java 8 特性：

| 特性 | 使用位置 | 实际代码示例 |
|------|---------|------------|
| **Lambda 表达式** | SecurityConfig | `csrf -> csrf.disable()` / `session -> session.sessionCreationPolicy(...)` |
| **方法引用** | LoginSuccessHandler | `userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority)` |
| **Stream API** | LoginSuccessHandler | `.stream().map(...).collect(Collectors.joining(","))` |
| **函数式接口** | SecurityConfig | `Customizer<CsrfConfigurer>`, `Customizer<SessionManagementConfigurer>` |

**实际代码片段：**

```java
// 1. Lambda 表达式 - SecurityConfig.java
http.sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

// 2. 方法引用 + Stream API - LoginSuccessHandler.java
String authorities = userDetails.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)    // 方法引用
        .collect(Collectors.joining(","));      // Stream 收集

// 3. Lambda 函数式接口 - JwtAuthenticationFilter.java
List<SimpleGrantedAuthority> authorities = Stream.of(authoritiesStr.split(","))
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
```

> **学习建议**：Java 8 的 Lambda 和 Stream API 在现代 Spring 项目中无处不在。本项目的实际代码是学习这些特性的最佳范例。

---

## 附录 D：项目当前状态说明

### 认证方式：Filter 链标准流程（已完成）

本项目 **jwt-java-eight** 采用 **Filter 链标准流程** 处理登录认证，而非传统的 Controller 手动驱动方式。

**实际实现方式：**

```
登录请求流程：
POST /auth/login → JwtLoginFilter → JwtAuthenticationProvider → LoginSuccessHandler → 返回双 Token

鉴权请求流程：
GET /api/xxx (携带 Token) → JwtAuthenticationFilter → 验证 Token → 设置 SecurityContext → Controller
```

**关键组件清单：**

| 组件 | 类型 | 职责 | 状态 |
|------|------|------|------|
| `JwtLoginFilter` | Filter | 拦截 `POST /auth/login`，触发认证流程 | ✅ 已实现 |
| `JwtAuthToken` | Token | 自定义认证令牌，与原生 Token 隔离 | ✅ 已实现 |
| `JwtAuthenticationProvider` | Provider | 自定义认证提供者，调用 UserDetailsService | ✅ 已实现 |
| `LoginSuccessHandler` | Handler | 认证成功后生成双 Token 并返回 | ✅ 已实现 |
| `LoginFailureHandler` | Handler | 认证失败后返回错误信息 | ✅ 已实现 |
| `JwtAuthenticationFilter` | Filter | 鉴权过滤器，解析已有 Token | ✅ 已实现 |
| `AuthController` | Controller | 只包含 register/refresh/logout 接口 | ✅ 已调整 |
| `AuthServiceImpl` | Service | 只包含 register/refresh/logout 方法 | ✅ 已调整 |

**与传统方式对比：**

| 维度 | 传统方式（Controller 驱动） | 本项目方式（Filter 驱动） |
|------|---------------------------|------------------------|
| 登录入口 | `AuthController.login()` | `JwtLoginFilter` 自动拦截 |
| Token 生成 | Service 层手动生成 | `LoginSuccessHandler` 自动处理 |
| 失败处理 | `@ExceptionHandler` 捕获 | `LoginFailureHandler` 自动处理 |
| 职责分离 | Controller 关心认证逻辑 | Controller 完全不知道认证细节 |
| 扩展性 | 修改 Service 代码 | 替换或新增 Handler |

### API 接口清单

**认证接口（/auth/**）：**

| 接口 | 方法 | 认证要求 | 处理方式 | 说明 |
|------|------|---------|---------|------|
| `/auth/login` | POST | 无 | `JwtLoginFilter` 自动拦截 | 用户名+密码登录，返回双 Token |
| `/auth/register` | POST | 无 | `AuthController.register()` | 用户注册 |
| `/auth/refresh` | POST | 无 | `AuthController.refresh()` | 刷新 Token（需提供 Refresh Token） |
| `/auth/logout` | POST | ✅ 需要 Token | `AuthController.logout()` | 清除 Refresh Token |

**受保护接口（/api/**）：**

所有 `/api/**` 路径下的接口都需要携带有效的 Access Token，由 `JwtAuthenticationFilter` 自动校验。

### 测试验证

**快速验证步骤：**

```bash
# 1. 启动项目
mvn spring-boot:run

# 2. 注册用户
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'

# 3. 登录获取 Token（由 JwtLoginFilter 处理）
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'

# 预期响应：
# {
#   "code": 200,
#   "message": "操作成功",
#   "data": {
#     "accessToken": "eyJ...",
#     "refreshToken": "eyJ...",
#     "tokenType": "Bearer"
#   }
# }
```

**验证清单：**

- ✅ 登录接口无需 Controller（Filter 自动处理）
- ✅ 认证成功返回双 Token
- ✅ 密码错误返回 401 错误
- ✅ Token 刷新正常
- ✅ 登出功能正常
- ✅ 鉴权过滤器正常工作

---

> **最后的话**
>
> JWT 认证看起来只是"生成一个字符串、验证一个字符串"这么简单，但真正的难点在于：
> - 如何设计安全的密钥管理？
> - 如何平衡安全性和用户体验？
> - 如何处理各种边界情况（Token被盗、用户被禁、并发刷新）？
>
> 这些问题没有标准答案，需要根据具体业务场景做出取舍。希望这份教程能帮助你理解背后的设计思想，而不仅仅是会写代码。
>
> **动手实践是最好的老师。** 现在，打开你的 IDE，从零开始跟着做一遍吧！
