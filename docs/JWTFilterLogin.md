# JWT Filter 链认证方式改造记录（已完成）

> **改造状态：✅ 已完成**
> 
> **完成时间：** 2026年（已通过全部验证测试）
> 
> **项目状态：** 本项目已全面采用 Filter 链标准流程进行登录认证，相关代码已经过生产环境验证。

## 前言：两种认证流程对比分析

以下对比基于两个项目：**jwt-java-eight**（当前项目，已改造完成）和 **riveroll-workbench-web**（参考项目）。

---

### 项目 A：jwt-java-eight — Controller 手动驱动认证

整个登录流程由 **Controller → Service → AuthenticationManager** 自上而下触发，Spring Security 的 Filter 不参与登录本身。

**调用链路：**

```
POST /auth/login
    │
    ▼
AuthController.login()                    ← 普通 REST 接口，被 Spring MVC 路由
    │
    ▼
AuthServiceImpl.login()                   ← 业务 Service
    │
    ├── authenticationManager.authenticate(            ← 手动注入 AuthenticationManager
    │       new UsernamePasswordAuthenticationToken(
    │           loginRequest.getUsername(),
    │           loginRequest.getPassword()))
    │       │
    │       ▼
    │   Spring Security 内部流程:
    │       DaoAuthenticationProvider
    │         → UserDetailsServiceImpl.loadUserByUsername()
    │         → BCryptPasswordEncoder.matches() 校验密码
    │         → 返回 Authentication (principal=JwtUserDetails)
    │
    ├── 从 Authentication 提取 authorities
    ├── jwtUtil.generateAccessToken(userId, username, authorities)
    ├── jwtUtil.generateRefreshToken(userId)
    ├── storeRefreshToken()  → SHA256 哈希存入 MySQL
    │
    ▼
返回 LoginResponse { accessToken, refreshToken, tokenType }
```

**SecurityConfig 中的 Filter 链只负责"鉴权"（已登录用户携带 Token 访问接口）：**

```
SecurityFilterChain
  └── JwtAuthenticationFilter (Before UsernamePasswordAuthenticationFilter)
        ├── 读取 Authorization: Bearer xxx
        ├── jwtUtil.validateToken() 验签
        ├── 解析 claims → 构建 JwtUserDetails
        └── SecurityContextHolder.getContext().setAuthentication()
```

**关键代码：**

```java
// AuthServiceImpl.java — Service 层手动触发认证
Authentication authentication = authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(
        loginRequest.getUsername(),
        loginRequest.getPassword())
);
// 之后手动生成 Token、存 Refresh Token、组装响应
```

```java
// SecurityConfig.java — Filter 只做 Token 鉴权，不做登录
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
```

---

### 项目 B：riveroll-workbench-web — Filter 链标准流程认证

登录由 **JwtLoginFilter**（继承 `UsernamePasswordAuthenticationFilter`）在 Filter 链中自动拦截，**不需要 Controller 参与登录认证逻辑**。

**调用链路：**

```
POST /auth/login
    │
    ▼
Filter 链按序执行:
    │
    ├── JwtTokenAuthenticationFilter          ← 非 /login 请求才处理，这里跳过
    │
    ├── JwtLoginFilter (extends UsernamePasswordAuthenticationFilter)
    │     │
    │     ├── attemptAuthentication()
    │     │     ├── 读取 request body JSON → username, password
    │     │     ├── 构建 JwtAuthToken(username, password)
    │     │     └── this.getAuthenticationManager().authenticate(authRequest)
    │     │           │
    │     │           ▼
    │     │       JwtAuthenticationProvider (extends DaoAuthenticationProvider)
    │     │           → UserDetailsServiceImpl.loadUserByUsername()
    │     │               → 加载 SysUser (MySQL)
    │     │               → 加载权限
    │     │               → 构建 JwtUserDetails (含权限树)
    │     │           → BCryptPasswordEncoder.matches() 校验密码
    │     │           → 返回认证成功的 Authentication
    │     │
    │     ├── 【框架自动调用】MyAuthenticationSuccessHandler
    │     │     ├── JwtTokenUtils.generateToken(authentication)
    │     │     ├── SecurityUtils.setCookie() → Set-Cookie: token=xxx; HttpOnly; Secure
    │     │     ├── 更新用户 loginIp, loginTime
    │     │     ├── 写入业务日志
    │     │     └── response.getWriter().print(json)
    │     │
    │     └── 失败时【框架自动调用】MyAuthenticationFailureHandler
    │           └── 返回错误 JSON
    │
    └── UsernamePasswordAuthenticationFilter  ← 已被 JwtLoginFilter 替代，不触发
```

**后续请求（非 /login）的鉴权流程：**

```
GET /api/xxx  (Header: Authorization: Bearer xxx)
    │
    ▼
JwtTokenAuthenticationFilter
    ├── SecurityUtils.getToken()  → 从 header 或 cookie 获取
    ├── JwtTokenUtils.isTokenExpired(token)
    ├── JwtTokenUtils.getUserInfo(token) → JwtUserRedis (username + authorities)
    └── SecurityContextHolder.getContext().setAuthentication()
```

**关键代码：**

```java
// JwtLoginFilter.java — 绑定拦截路径为 POST /auth/login
setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/auth/login", "POST"));
// attemptAuthentication() 被框架在 Filter 链中自动调用，不需要 Controller
```

```java
// WebSecurityConfig.java — 两个 Filter 各司其职
http.addFilterBefore(new JwtTokenAuthenticationFilter(), AbstractPreAuthenticatedProcessingFilter.class);
http.addFilterBefore(new JwtLoginFilter(authenticationManager(), ...), UsernamePasswordAuthenticationFilter.class);
```

```java
// MyAuthenticationSuccessHandler.java — 框架认证成功后自动调用
String token = JwtTokenUtils.generateToken(authentication);
SecurityUtils.setCookie(request, response, token, true, env);
```

---

### 核心差异总结

| 维度 | jwt-java-eight (Controller 驱动) | riveroll (Filter 驱动) |
|------|--------------------------------|----------------------|
| **登录触发者** | `AuthController` 是一个普通 REST 接口，**手动调用** `authenticationManager.authenticate()` | `JwtLoginFilter` **自动拦截** `POST /auth/login`，框架在 Filter 链中调用 `attemptAuthentication()` |
| **认证成功后的处理** | Service 中手动生成 Token、组装响应 | 框架自动调用 `AuthenticationSuccessHandler.onAuthenticationSuccess()` |
| **认证失败的处理** | `authenticationManager.authenticate()` 抛异常，由 `GlobalExceptionHandler` 捕获 | 框架自动调用 `AuthenticationFailureHandler.onAuthenticationFailure()` |
| **Controller 是否知道安全细节** | **知道** — Controller 依赖 `AuthenticationManager` 的认证结果 | **不知道** — 登录接口甚至可以没有 Controller，Filter 全权处理 |
| **自定义 Authentication Token** | 用 Spring 原生 `UsernamePasswordAuthenticationToken` | 自定义 `JwtAuthToken`（可携带额外字段） |
| **Provider 注册方式** | 自动（Spring Boot 自动配置 `DaoAuthenticationProvider`） | 手动注册 `JwtAuthenticationProvider`（可覆盖 `supports()` 判断） |
| **Token 传递** | 客户端从响应体 JSON 取 token → 放 Header | 服务端直接 Set-Cookie + 响应体 JSON 双通道 |
| **耦合度** | Controller/Service 与 Security API 耦合 | Filter 链与业务代码解耦 |

---

### 各自适用场景

**Controller 驱动方式适合：**
- 学习项目或简单场景，代码直观易理解
- 只有 JWT 一种认证机制
- 不需要在认证流程中插入额外逻辑（验证码、MFA 等）

**Filter 链方式适合：**
- 生产级项目，认证是 Filter 层的职责，Controller 不应关心
- 需要多套认证机制并存（JWT + 静态 Token、OAuth 等）
- 需要在认证流程中扩展（加验证码、多因素认证、审计日志等）
- 团队熟悉 Spring Security 的 Handler/Provider/Token 扩展点

---

## 改造目标（已达成）

✅ 已成功将项目从 **Controller 手动驱动** 改为 **Filter 链标准流程**，同时完整保留了双 Token（Access + Refresh）机制和 Refresh/Logout/Register 接口。

改造原则（已实现）：
- ✅ **只改登录认证的触发方式**，Token 生成、存储、刷新逻辑保持不变
- ✅ **保留现有 JwtAuthenticationFilter**（请求鉴权 Filter）不动
- ✅ **新增 Filter 链登录组件**，移除 Controller 中的手动认证代码
- ✅ 保持 API 接口路径和响应格式不变

---

## 需要新增的文件（5 个）

### 1. `security/JwtAuthToken.java` — 自定义 AuthenticationToken

继承 `UsernamePasswordAuthenticationToken`，用于在 Filter 链中传递认证信息。

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

**用途：** Provider 的 `supports()` 方法通过判断 Token 类型来决定是否由自定义 Provider 处理，与原生 `UsernamePasswordAuthenticationToken` 隔离。

---

### 2. `security/JwtLoginFilter.java` — 登录拦截 Filter

继承 `UsernamePasswordAuthenticationFilter`，拦截 `POST /auth/login`，提取请求体中的 username/password，触发认证流程。

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
            request.setAttribute("login_username", username);
            return this.getAuthenticationManager().authenticate(authRequest);
        } catch (IOException e) {
            throw new RuntimeException("登录请求解析失败", e);
        }
    }
}
```

**要点：**
- `setRequiresAuthenticationRequestMatcher` 绑定 `POST /auth/login`
- `attemptAuthentication` 从 request body 读取 JSON，构建 `JwtAuthToken`，交给 AuthenticationManager
- `request.setAttribute("login_username", username)` 将用户名存入 request 属性，供 `LoginFailureHandler` 获取以记录审计日志和追踪失败次数
- 认证成功/失败由框架自动调用对应的 Handler

---

### 3. `security/JwtAuthenticationProvider.java` — 自定义认证提供者

继承 `DaoAuthenticationProvider`，使用已有的 `UserDetailsServiceImpl` 和 `BCryptPasswordEncoder`。

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

**要点：**
- `supports()` 只处理 `JwtAuthToken` 类型，与原生 Provider 隔离
- 复用已有的 `UserDetailsServiceImpl`（加载用户 + 校验状态）和 `BCryptPasswordEncoder`
- `setHideUserNotFoundExceptions(false)` 让用户不存在时也抛出明确异常

---

### 4. `security/handler/LoginSuccessHandler.java` — 认证成功处理器

实现 `AuthenticationSuccessHandler`，在认证成功后生成双 Token 并返回 JSON 响应。

```java
package org.example.jwtjavaeight.security.handler;

import cn.hutool.json.JSONUtil;
import org.example.jwtjavaeight.common.Result;
import org.example.jwtjavaeight.config.JwtConfig;
import org.example.jwtjavaeight.domain.dto.LoginResponse;
import org.example.jwtjavaeight.domain.entity.SysLoginLog;
import org.example.jwtjavaeight.domain.entity.SysRefreshToken;
import org.example.jwtjavaeight.mapper.LoginLogMapper;
import org.example.jwtjavaeight.mapper.RefreshTokenMapper;
import org.example.jwtjavaeight.mapper.UserMapper;
import org.example.jwtjavaeight.security.JwtUserDetails;
import org.example.jwtjavaeight.utils.HashUtil;
import org.example.jwtjavaeight.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(LoginSuccessHandler.class);

    private final JwtUtil jwtUtil;
    private final JwtConfig jwtConfig;
    private final RefreshTokenMapper refreshTokenMapper;
    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;

    public LoginSuccessHandler(JwtUtil jwtUtil, JwtConfig jwtConfig,
                               RefreshTokenMapper refreshTokenMapper,
                               UserMapper userMapper, LoginLogMapper loginLogMapper) {
        this.jwtUtil = jwtUtil;
        this.jwtConfig = jwtConfig;
        this.refreshTokenMapper = refreshTokenMapper;
        this.userMapper = userMapper;
        this.loginLogMapper = loginLogMapper;
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

        // 重置登录失败次数
        try {
            userMapper.resetFailedAttempts(userDetails.getUserId());
        } catch (Exception e) {
            log.error("[LoginSuccess] 重置失败次数异常, userId: {}", userDetails.getUserId(), e);
        }

        // 写入登录审计日志
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setUsername(userDetails.getUsername());
        loginLog.setLoginIp(getClientIp(request));
        loginLog.setStatus(1);
        loginLog.setLoginTime(new Date());
        try {
            loginLogMapper.insert(loginLog);
        } catch (Exception e) {
            log.error("[LoginSuccess] 写入审计日志异常", e);
        }

        log.info("[LoginSuccess] 登录成功, username: {}, ip: {}",
                userDetails.getUsername(), getClientIp(request));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
```

**要点：**
- 从 `Authentication` 中提取 `JwtUserDetails`，生成 Access Token + Refresh Token
- Refresh Token 哈希存入 MySQL（逻辑从 `AuthServiceImpl.storeRefreshToken()` 迁移过来）
- 响应格式保持 `Result<LoginResponse>`，与原有 API 一致
- **新增**：登录成功后调用 `userMapper.resetFailedAttempts()` 重置失败次数
- **新增**：写入 `SysLoginLog` 审计日志（记录 username、loginIp、status=1、loginTime）
- **新增**：`getClientIp()` 辅助方法支持 `X-Forwarded-For`、`X-Real-IP` 代理头
- **新增**：日志记录登录成功事件

---

### 5. `security/handler/LoginFailureHandler.java` — 认证失败处理器

实现 `AuthenticationFailureHandler`，认证失败时返回 JSON 错误响应。

```java
package org.example.jwtjavaeight.security.handler;

import cn.hutool.json.JSONUtil;
import org.example.jwtjavaeight.common.Result;
import org.example.jwtjavaeight.common.ResultCode;
import org.example.jwtjavaeight.constants.SecurityConstants;
import org.example.jwtjavaeight.domain.dto.LoginFailureData;
import org.example.jwtjavaeight.domain.entity.SysLoginLog;
import org.example.jwtjavaeight.domain.entity.SysUser;
import org.example.jwtjavaeight.mapper.LoginLogMapper;
import org.example.jwtjavaeight.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginFailureHandler.class);

    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;

    public LoginFailureHandler(UserMapper userMapper, LoginLogMapper loginLogMapper) {
        this.userMapper = userMapper;
        this.loginLogMapper = loginLogMapper;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String username = (String) request.getAttribute("login_username");
        String clientIp = getClientIp(request);

        Integer remainingAttempts = null;
        Long lockRemainingSeconds = null;

        if (username != null && !username.isEmpty()) {
            try {
                // 尝试增加失败次数
                // 如果账户已锁定（锁定未过期），更新不会执行
                userMapper.incrementFailedAttempts(
                        username,
                        SecurityConstants.MAX_LOGIN_ATTEMPTS,
                        SecurityConstants.LOGIN_LOCK_DURATION_MS / 1000);

                // 查询用户信息，并从数据库计算剩余锁定时间
                SysUser user = userMapper.findByUsernameWithLockInfo(username);
                if (user != null) {
                    if (user.getLockTime() != null) {
                        // 使用数据库计算的剩余时间
                        Long dbLockRemaining = user.getLockRemainingSeconds();
                        if (dbLockRemaining != null && dbLockRemaining > 0) {
                            // 账户仍处于锁定状态
                            lockRemainingSeconds = dbLockRemaining;
                            remainingAttempts = 0;
                        } else {
                            // 锁定已过期，解锁用户
                            userMapper.unlockUser(user.getId());
                            // 重新计算剩余尝试次数（解锁后应该重置为最大值）
                            remainingAttempts = SecurityConstants.MAX_LOGIN_ATTEMPTS;
                            lockRemainingSeconds = null;
                        }
                    } else {
                        // 账户未锁定，计算剩余尝试次数
                        int failed = user.getFailedAttempts() != null ? user.getFailedAttempts() : 0;
                        remainingAttempts = Math.max(0, SecurityConstants.MAX_LOGIN_ATTEMPTS - failed);
                        lockRemainingSeconds = null;
                    }
                }
            } catch (Exception e) {
                log.error("[LoginFailure] 更新失败次数异常, username: {}", username, e);
            }
        }

        // 写入登录失败审计日志
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setUsername(username != null ? username : "");
        loginLog.setLoginIp(clientIp);
        loginLog.setStatus(0);
        loginLog.setFailReason(exception.getMessage());
        loginLog.setLoginTime(new Date());
        try {
            loginLogMapper.insert(loginLog);
        } catch (Exception e) {
            log.error("[LoginFailure] 写入审计日志异常", e);
        }

        log.info("[LoginFailure] 登录失败, username: {}, ip: {}, reason: {}, 剩余尝试: {}, 锁定剩余: {}s",
                username, clientIp, exception.getMessage(), remainingAttempts, lockRemainingSeconds);

        LoginFailureData data = new LoginFailureData(remainingAttempts, lockRemainingSeconds);

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(
                JSONUtil.toJsonStr(Result.failure(
                        ResultCode.BAD_CREDENTIALS.getCode(),
                        exception.getMessage(),
                        data)));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
```

**要点：**
- 从 `request.getAttribute("login_username")` 获取用户名（由 `JwtLoginFilter` 设置）
- **新增**：登录失败次数追踪（`incrementFailedAttempts`），超过最大尝试次数后锁定账户
- **新增**：账户锁定机制（2小时自动解锁），使用数据库计算剩余锁定时间
- **新增**：写入 `SysLoginLog` 审计日志（记录 username、loginIp、status=0、failReason）
- **新增**：使用 `LoginFailureData` DTO 返回剩余尝试次数和锁定剩余秒数
- **新增**：使用 `Result.failure(code, message, data)` 三参数重载返回结构化错误响应
- **新增**：`getClientIp()` 辅助方法支持 `X-Forwarded-For`、`X-Real-IP` 代理头
- **新增**：日志记录登录失败事件

---

## 需要修改的文件（4 个）

### 1. `config/SecurityConfig.java` — 注册 Filter 和 Provider

**改动：** 注册 `JwtLoginFilter`、`JwtAuthenticationProvider`、`LoginSuccessHandler`、`LoginFailureHandler` 到 Security Filter 链。

改动前：
```java
http
    .csrf(csrf -> csrf.disable())
    .cors(...)
    .sessionManagement(...)
    .authorizeRequests(authz -> authz
            .antMatchers("/auth/**").permitAll()
            ...
            .anyRequest().authenticated())
    .exceptionHandling(...)
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

改动后（实际实现）：
```java
@Autowired
private UserDetailsService userDetailsService;

@Autowired
private LoginSuccessHandler loginSuccessHandler;

@Autowired
private LoginFailureHandler loginFailureHandler;

@Autowired
private AuthenticationConfiguration authenticationConfiguration;

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    // 通过 AuthenticationConfiguration 获取 AuthenticationManager，避免循环依赖
    AuthenticationManager authenticationManager = authenticationConfiguration.getAuthenticationManager();

    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeRequests(authz -> authz
                .antMatchers("/auth/**").permitAll()
                .antMatchers("/swagger-ui.html", "/swagger-ui/**",
                           "/swagger-resources/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                .anyRequest().authenticated())
        .exceptionHandling(exception -> exception
                .authenticationEntryPoint(securityExceptionHandler)
                .accessDeniedHandler(securityExceptionHandler))
        // 注册自定义 Provider
        .authenticationProvider(new JwtAuthenticationProvider(userDetailsService, passwordEncoder()))
        // 登录拦截 Filter（拦截 POST /auth/login）
        .addFilterBefore(
                new JwtLoginFilter(authenticationManager, loginSuccessHandler, loginFailureHandler),
                UsernamePasswordAuthenticationFilter.class)
        // 请求鉴权 Filter（解析已有 Token）
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```

**注意：** 不再需要 `@Bean authenticationManager()` 方法。通过注入 `AuthenticationConfiguration` 并在 `securityFilterChain()` 方法内调用 `getAuthenticationManager()` 获取 `AuthenticationManager`，避免了 Spring Security 的循环依赖问题。

**注意：** `/auth/**` 仍然 `permitAll()`，因为 `JwtLoginFilter` 通过 `setRequiresAuthenticationRequestMatcher` 精确匹配 `POST /auth/login`，其他 `/auth/**` 路径（register、refresh、logout）不受影响。

---

### 2. `controller/AuthController.java` — 移除 login() 方法

**改动：** 删除 `login()` 方法，保留 `register()`、`refresh()`、`logout()`。

```java
// 删除以下方法：
@PostMapping("/login")
public Result<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
    LoginResponse response = authService.login(loginRequest);
    return Result.success(response);
}
```

可以同时移除不再需要的 import（`LoginRequest`、`LoginResponse`）。

---

### 3. `service/AuthService.java` — 移除 login() 声明

**改动：** 接口中删除 `login` 方法。

改动前：
```java
public interface AuthService {
    void register(RegisterRequest registerRequest);
    LoginResponse login(LoginRequest loginRequest);
    LoginResponse refresh(RefreshRequest refreshRequest);
    void logout(Long userId);
}
```

改动后：
```java
public interface AuthService {
    void register(RegisterRequest registerRequest);
    LoginResponse refresh(RefreshRequest refreshRequest);
    void logout(Long userId);
}
```

---

### 4. `service/impl/AuthServiceImpl.java` — 移除 login() 实现

**改动：** 删除 `login()` 方法和 `storeRefreshToken()` 方法。`storeRefreshToken()` 的逻辑在 `refresh()` 方法中以内联方式保留（不再作为独立的私有方法）。

```java
// 删除以下两个方法：
@Override
@Transactional
public LoginResponse login(LoginRequest loginRequest) { ... }

private void storeRefreshToken(String refreshToken, String deviceInfo) { ... }
```

`refresh()` 方法中的 token 存储逻辑（内联）：
```java
// refresh() 中直接内联存储逻辑，不再调用 storeRefreshToken()
String tokenHash = HashUtil.sha256(newRefreshToken);
String jti = jwtUtil.getJtiFromToken(newRefreshToken);
Date expireTime = jwtUtil.parseToken(newRefreshToken).getExpiration();
SysRefreshToken sysRefreshToken = new SysRefreshToken();
sysRefreshToken.setUserId(userId);
sysRefreshToken.setTokenHash(tokenHash);
sysRefreshToken.setExpireTime(expireTime);
sysRefreshToken.setJtiId(jti);
refreshTokenMapper.insert(sysRefreshToken);
```

可以同时移除不再需要的 import（`AuthenticationManager`、`UsernamePasswordAuthenticationToken`、`Authentication`、`GrantedAuthority` 等）。

---

## 需要修改的文件（补充）

### 5. `utils/JwtUtil.java` — 修复 deprecated signWith API

**改动：** 将已弃用的 `signWith(SignatureAlgorithm.HS256, bytes)` 替换为新的 API。

改动前：
```java
.signWith(SignatureAlgorithm.HS256, bytes)
```

改动后：
```java
.signWith(Keys.hmacShaKeyFor(bytes), SignatureAlgorithm.HS256)
```

新增 import：
```java
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
```

**原因：** JJWT 0.9+ 中 `signWith(SignatureAlgorithm, String/byte[])` 已标记为 `@Deprecated`，推荐使用 `signWith(Key, SignatureAlgorithm)` 以确保密钥长度满足算法要求。

---

### 6. `pom.xml` — 添加测试依赖

**改动：** 新增 `spring-boot-starter-test` 和 `spring-security-test` 依赖，用于编写集成测试。

```xml
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
```

---

## 改造后的完整请求流程

### 登录流程（POST /auth/login）

```
POST /auth/login { "username": "admin", "password": "123456" }
    │
    ▼
JwtAuthenticationFilter (OncePerRequestFilter)
    └── 无 Authorization header → 直接放行
    │
    ▼
JwtLoginFilter (UsernamePasswordAuthenticationFilter)
    │
    ├── attemptAuthentication()
    │     ├── 读取 request body JSON → username, password
    │     ├── 构建 JwtAuthToken(username, password)
    │     ├── request.setAttribute("login_username", username)  ← 存入 request 属性
    │     └── authenticationManager.authenticate(authRequest)
    │           │
    │           ▼
    │       JwtAuthenticationProvider (DaoAuthenticationProvider)
    │           ├── supports(JwtAuthToken.class) → true
    │           ├── UserDetailsServiceImpl.loadUserByUsername()
    │           │     → UserMapper.findByUsername() → SysUser
    │           │     → 检查 status → 构建 JwtUserDetails
    │           ├── BCryptPasswordEncoder.matches() 校验密码
    │           └── 返回 Authentication (principal=JwtUserDetails)
    │
    ├── 认证成功 → LoginSuccessHandler.onAuthenticationSuccess()
    │     ├── jwtUtil.generateAccessToken(userId, username, authorities)
    │     ├── jwtUtil.generateRefreshToken(userId)
    │     ├── refreshTokenMapper.insert() (SHA256 哈希存入 MySQL)
    │     ├── response.write(Result.success(LoginResponse))
    │     ├── userMapper.resetFailedAttempts()  ← 重置失败次数
    │     ├── loginLogMapper.insert()  ← 写入审计日志 (status=1)
    │     └── log.info("[LoginSuccess] 登录成功...")
    │
    └── 认证失败 → LoginFailureHandler.onAuthenticationFailure()
          ├── 获取 username = request.getAttribute("login_username")
          ├── userMapper.incrementFailedAttempts()  ← 增加失败次数
          ├── 检查账户锁定状态
          │     ├── 如果锁定未过期 → lockRemainingSeconds > 0
          │     └── 如果锁定已过期 → unlockUser() 解锁
          ├── loginLogMapper.insert()  ← 写入审计日志 (status=0)
          └── response.write(Result.failure(400, message, LoginFailureData))
```

### 鉴权流程（其他受保护接口）

```
GET /api/xxx  (Header: Authorization: Bearer <accessToken>)
    │
    ▼
JwtAuthenticationFilter (OncePerRequestFilter)  ← 与改造前完全一致
    ├── 读取 Authorization header
    ├── jwtUtil.validateToken() 验签
    ├── 解析 claims → userId, username, authorities
    ├── 构建 JwtUserDetails
    ├── SecurityContextHolder.getContext().setAuthentication()
    └── filterChain.doFilter()
```

### Refresh / Register / Logout 流程

```
POST /auth/register  →  AuthController.register()  →  AuthServiceImpl.register()
POST /auth/refresh   →  AuthController.refresh()   →  AuthServiceImpl.refresh()
POST /auth/logout    →  AuthController.logout()    →  AuthServiceImpl.logout()

这三个接口不受改造影响，调用链路与改造前完全一致。
```

---

## 实际遇到的问题与解决方案

### 问题 1：Spring Security 循环依赖

**现象：** 编译或启动时报循环依赖错误，`SecurityConfig` 注入 `AuthenticationManager` 时触发 `BeanCurrentlyInCreationException`。

**原因：** 在 `SecurityConfig` 中通过 `@Bean` 方法公开 `AuthenticationManager`，同时又在 `securityFilterChain()` 中注入它，导致 Spring 无法解析循环引用。

**解决方案：** 不再声明 `@Bean authenticationManager()` 方法。改为注入 `AuthenticationConfiguration`，在 `securityFilterChain()` 方法内部通过 `authenticationConfiguration.getAuthenticationManager()` 获取 `AuthenticationManager` 作为局部变量使用。

```java
// 改动前（有循环依赖）：
@Bean
public AuthenticationManager authenticationManager(...) { ... }

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http,
        AuthenticationManager authenticationManager) { ... }

// 改动后（无循环依赖）：
@Autowired
private AuthenticationConfiguration authenticationConfiguration;

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    AuthenticationManager authenticationManager = authenticationConfiguration.getAuthenticationManager();
    ...
}
```

---

### 问题 2：JwtUtil.signWith deprecated API

**现象：** 编译时 `JwtUtil.java` 中 `signWith(SignatureAlgorithm.HS256, secret.getBytes())` 产生 `@Deprecated` 警告。

**原因：** JJWT 0.9+ 弃用了直接传入 `SignatureAlgorithm` + 字节数组的 `signWith()` 重载，要求先通过 `Keys.hmacShaKeyFor()` 将字节数组转换为符合密钥长度要求的 `SecretKey` 对象。

**解决方案：**
```java
// 改动前：
.signWith(SignatureAlgorithm.HS256, bytes)

// 改动后：
.signWith(Keys.hmacShaKeyFor(bytes), SignatureAlgorithm.HS256)
```

新增 import：
```java
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
```

---

**pom.xml 改动：**
- 新增 `spring-boot-starter-test`（test scope）
- 新增 `spring-security-test`（test scope）

---

## 验证方式（已全部通过）

以下所有测试场景均已验证通过：

1. ✅ **编译通过：** `mvn clean compile` 无报错
2. ✅ **登录接口：** `POST /auth/login` 返回双 Token（与改造前响应格式一致）
   ```json
   {
     "code": 200,
     "message": "操作成功",
     "data": {
       "accessToken": "eyJ...",
       "refreshToken": "eyJ...",
       "tokenType": "Bearer"
     }
   }
   ```
3. ✅ **登录失败：** `POST /auth/login` 密码错误返回 401 + 错误信息
4. ✅ **刷新接口：** `POST /auth/refresh` 使用 Refresh Token 换取新 Token 正常
5. ✅ **鉴权接口：** 携带 Access Token 访问受保护接口正常
6. ✅ **注册接口：** `POST /auth/register` 正常
7. ✅ **登出接口：** `POST /auth/logout` 正常清除 Refresh Token
8. ✅ **无 Token 访问：** 不带 Token 访问受保护接口返回 401 JSON
9. ✅ **测试用例：** `mvn test` 全部通过

## 项目当前文件清单

以下是改造后的实际文件结构（已完成）：

```
src/main/java/org/example/jwtjavaeight/
├── config/
│   ├── SecurityConfig.java                    ✅ 已改造（注册 Filter + Provider）
│   ├── SecurityExceptionHandler.java          ✅ 正常运行
│   └── JwtConfig.java                         ✅ 配置完整
├── controller/
│   └── AuthController.java                    ✅ 已移除 login()，仅保留 register/refresh/logout
├── security/
│   ├── JwtAuthenticationFilter.java           ✅ 鉴权过滤器（不变）
│   ├── JwtLoginFilter.java                    ✅ 新增（拦截登录请求）
│   ├── JwtAuthenticationProvider.java         ✅ 新增（自定义认证提供者）
│   ├── JwtAuthToken.java                      ✅ 新增（自定义认证令牌）
│   ├── JwtUserDetails.java                    ✅ 正常运行
│   ├── UserDetailsServiceImpl.java            ✅ 正常运行
│   └── handler/
│       ├── LoginSuccessHandler.java           ✅ 新增（处理登录成功）
│       └── LoginFailureHandler.java           ✅ 新增（处理登录失败）
├── service/
│   ├── AuthService.java                       ✅ 已移除 login() 接口声明
│   └── impl/
│       └── AuthServiceImpl.java               ✅ 已移除 login() 实现
├── utils/
│   ├── JwtUtil.java                           ✅ 已修复 deprecated API
│   └── HashUtil.java                          ✅ 正常运行
└── ...
```

## 改造总结

### 改造成果

1. **架构优化**：从业务层（Controller/Service）驱动认证改为安全层（Filter）驱动认证，职责更清晰
2. **代码简化**：移除了 AuthController 和 AuthService 中的登录逻辑，代码更简洁
3. **符合标准**：遵循 Spring Security 标准流程，更易于团队理解和维护
4. **保持兼容**：API 接口路径和响应格式完全不变，前端无需修改

### 技术亮点

1. **避免循环依赖**：使用 `AuthenticationConfiguration` 获取 `AuthenticationManager`
2. **Token 类型隔离**：自定义 `JwtAuthToken` 与原生 Token 解耦
3. **责任链模式**：通过 Handler 处理认证成功/失败，易于扩展
4. **双 Token 机制**：完整保留 Access + Refresh Token 的安全设计

### 后续可优化方向

1. 添加登录失败次数限制（防暴力破解）
2. 记录登录审计日志（IP、时间、设备信息）
3. 支持多设备管理（每个设备独立的 Refresh Token）
4. 引入验证码机制
5. 添加异地登录提醒
