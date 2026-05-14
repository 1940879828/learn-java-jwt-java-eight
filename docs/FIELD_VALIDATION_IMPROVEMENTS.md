# 字段校验与错误处理改进

## 问题现象

登录时前端发送了`remember`字段，但后端的`LoginRequest`没有该字段，导致：
```
UnrecognizedPropertyException: Unrecognized field "remember"
```
错误返回为通用的"未登录或Token无效"，前端无法知道具体是哪个字段出错。

## 解决方案（四层防护）

### 第1层：添加缺失字段到LoginRequest

```java
public class LoginRequest {
    private String username;
    private String password;
    private Boolean remember;  // ✅ 新增：记住我功能
}
```

**作用**: 支持前端"记住我"功能，为后续扩展token有效期做准备。

---

### 第2层：改进JwtLoginFilter错误处理

```java
@Override
public Authentication attemptAuthentication(...) {
    try {
        LoginRequest loginReq = objectMapper.readValue(...);
        // 处理记住我逻辑
        if (loginReq.getRemember()) {
            request.setAttribute("remember_me", true);
        }
        // ...
    } catch (UnrecognizedPropertyException e) {
        // ✅ 捕获并友好提示
        throw new BadCredentialsException("请求参数错误：字段 '" + e.getPropertyName() + "' 不被识别");
    } catch (InvalidFormatException e) {
        // ✅ 字段格式错误
        throw new BadCredentialsException("请求参数错误：字段 '" + fieldName + "' 格式不正确");
    } catch (MismatchedInputException e) {
        // ✅ 必填字段缺失
        throw new BadCredentialsException("请求参数错误：缺少必填字段");
    }
}
```

**作用**: 在登录过滤器层面捕获JSON解析异常，返回友好的错误信息。

---

### 第3层：增强全局异常处理器

新增以下异常处理：

#### 3.1 JSON请求体解析错误
```java
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<Result<Void>> handleHttpMessageNotReadable(...) {
    // 详细区分错误类型
    if (cause instanceof UnrecognizedPropertyException) {
        message = "未知字段: 'xxx'";
    } else if (cause instanceof InvalidFormatException) {
        message = "字段 'xxx' 格式错误，期望类型: Integer";
    } else if (cause instanceof MismatchedInputException) {
        message = "字段 'xxx' 值不匹配或缺失";
    }
}
```

#### 3.2 字段校验错误（@Valid）
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Result<Void>> handleValidation(...) {
    // ✅ 改进：显示当前值
    message = "username: 长度必须在3到20之间 (当前值: ab)";
}
```

#### 3.3 表单绑定错误
```java
@ExceptionHandler(BindException.class)
public ResponseEntity<Result<Void>> handleBindException(...) {
    // 处理表单提交的验证错误
}
```

#### 3.4 约束违反异常
```java
@ExceptionHandler(ConstraintViolationException.class)
public ResponseEntity<Result<Void>> handleConstraintViolation(...) {
    // 处理方法级别的@Validated验证
}
```

**作用**: 统一处理所有类型的字段校验错误，返回清晰的错误信息。

---

### 第4层：Jackson全局配置（兜底）

```java
@Configuration
public class WebMvcConfiguration {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // ✅ 忽略未知字段（防止前端多传字段导致错误）
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // ✅ 允许空字符串转null
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        
        // ✅ 设置日期格式
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        
        return mapper;
    }
}
```

**作用**: 全局配置Jackson行为，即使某些异常没被捕获，也能容错处理。

---

## 错误返回格式

### 之前 ❌
```json
{
  "code": 40100,
  "message": "未登录或Token无效",
  "timestamp": 1778732520354
}
```
**问题**: 无法知道具体是什么错误。

### 之后 ✅

#### 示例1：未知字段
```json
{
  "code": 40000,
  "message": "未知字段: 'remember'",
  "data": null,
  "traceId": "xxx",
  "timestamp": "2026-05-14T12:00:00+08:00"
}
```

#### 示例2：字段格式错误
```json
{
  "code": 40000,
  "message": "字段 'age' 格式错误，期望类型: Integer",
  "data": null,
  "traceId": "xxx",
  "timestamp": "2026-05-14T12:00:00+08:00"
}
```

#### 示例3：字段校验失败
```json
{
  "code": 40000,
  "message": "username: 长度必须在3到20之间 (当前值: ab); password: 密码长度至少6位",
  "data": null,
  "traceId": "xxx",
  "timestamp": "2026-05-14T12:00:00+08:00"
}
```

---

## 验证测试

### 测试1：未知字段
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456","unknownField":"value"}'
```
**预期**: 正常登录成功（忽略未知字段）

### 测试2：字段格式错误
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":12345}'
```
**预期**: 返回"字段 'password' 格式错误，期望类型: String"

### 测试3：缺少必填字段
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin"}'
```
**预期**: 返回"password: 密码不能为空"

### 测试4：字段长度校验
```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"username":"ab","password":"123456","email":"test@test.com"}'
```
**预期**: 返回"username: 长度必须在3到20之间 (当前值: ab)"

---

## 日志增强

现在所有字段校验错误都会记录日志：

```log
2026-05-14 12:00:00.123  WARN [ValidationError] 字段校验失败: username: 长度必须在3到20之间 (当前值: ab)
2026-05-14 12:00:01.456  WARN [JsonParseError] 请求包含未知字段: unknownField
2026-05-14 12:00:02.789  WARN [JsonParseError] 字段格式错误: password -> String
2026-05-14 12:00:03.012  WARN [BindError] 表单绑定失败: age: 必须是数字
```

---

## 扩展：记住我功能

现在`LoginRequest`已支持`remember`字段，后续可以扩展：

```java
// LoginSuccessHandler
if (Boolean.TRUE.equals(request.getAttribute("remember_me"))) {
    // 生成7天有效期的token
    String longTermToken = jwtUtil.generateLongTermToken(userId, username, authorities);
    // ...
}
```

---

## 最佳实践

### DTO设计原则
1. ✅ 所有可能从前端接收的字段都应该在DTO中定义
2. ✅ 使用`@Schema`注解标注字段说明和示例值
3. ✅ 使用`@NotBlank`, `@Size`, `@Pattern`等注解做字段校验
4. ✅ 可选字段使用包装类型（Boolean, Integer）而非基本类型

### 错误处理原则
1. ✅ 在最外层（Filter）捕获并转换异常
2. ✅ 在全局异常处理器中统一格式化错误信息
3. ✅ 错误信息要明确指出是哪个字段出错、为什么出错
4. ✅ 生产环境不要暴露敏感的堆栈信息

### Jackson配置原则
1. ✅ 默认忽略未知字段（前端兼容性）
2. ✅ 统一日期格式和时区
3. ✅ 开发环境可格式化JSON，生产环境压缩
4. ✅ 使用自定义ObjectMapper而非默认配置
