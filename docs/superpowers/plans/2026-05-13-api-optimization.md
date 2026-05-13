# API Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Completely refactor the JWT authentication system API according to P0-P3 optimization guidelines, fixing security vulnerabilities, establishing RESTful standards, and building a complete CRUD system with error handling and validation.

**Architecture:** Direct replacement strategy - migrate all endpoints to `/api/v1/` paths, introduce DTO layer to separate request/response from entities, implement comprehensive error handling with custom error codes, enhance security with token rotation and login throttling, add pagination and filtering to all list endpoints.

**Tech Stack:** Spring Boot 2.7.6, Spring Security 5.7.5, JWT 0.11.5, MyBatis 2.2.2, MySQL 5.7.44, Java 8

**Constraints:**
- SQL files go to `docs/sql/` directory (do not execute)
- Test password: `$2a$10$SG7HkhWRQvmDzCG3S.aYsOxxezisRSSm.jRm4uXszZMdpf0g3JuMO`
- Skip Redis-related tests
- Do not compile or run the project

---

## File Structure Map

### New Files to Create

**Enums:**
- `src/main/java/org/example/jwtjavaeight/enums/ErrorCode.java`
- `src/main/java/org/example/jwtjavaeight/enums/DataScopeEnum.java`
- `src/main/java/org/example/jwtjavaeight/enums/MenuTypeEnum.java`

**DTOs - User:**
- `src/main/java/org/example/jwtjavaeight/domain/dto/UserCreateRequest.java`
- `src/main/java/org/example/jwtjavaeight/domain/dto/UserUpdateRequest.java`
- `src/main/java/org/example/jwtjavaeight/domain/dto/UserResponse.java`
- `src/main/java/org/example/jwtjavaeight/domain/dto/UserDetailResponse.java`
- `src/main/java/org/example/jwtjavaeight/domain/dto/ChangePasswordRequest.java`
- `src/main/java/org/example/jwtjavaeight/domain/dto/LockUserRequest.java`
- `src/main/java/org/example/jwtjavaeight/domain/dto/ResetPasswordRequest.java`

**DTOs - Role:**
- `src/main/java/org/example/jwtjavaeight/domain/dto/RoleCreateRequest.java`
- `src/main/java/org/example/jwtjavaeight/domain/dto/RoleUpdateRequest.java`
- `src/main/java/org/example/jwtjavaeight/domain/dto/RoleResponse.java`

**DTOs - Menu:**
- `src/main/java/org/example/jwtjavaeight/domain/dto/MenuCreateRequest.java`
- `src/main/java/org/example/jwtjavaeight/domain/dto/MenuUpdateRequest.java`
- `src/main/java/org/example/jwtjavaeight/domain/dto/MenuResponse.java`
- `src/main/java/org/example/jwtjavaeight/domain/dto/MenuTreeNode.java`

**DTOs - Common:**
- `src/main/java/org/example/jwtjavaeight/domain/dto/PageResponse.java`
- `src/main/java/org/example/jwtjavaeight/domain/dto/PageRequest.java`
- `src/main/java/org/example/jwtjavaeight/domain/dto/UserQueryFilter.java`
- `src/main/java/org/example/jwtjavaeight/domain/dto/RoleQueryFilter.java`
- `src/main/java/org/example/jwtjavaeight/domain/dto/MenuQueryFilter.java`

**Exceptions:**
- `src/main/java/org/example/jwtjavaeight/exception/ResourceNotFoundException.java`

**SQL:**
- `docs/sql/V003__test_data.sql`

### Files to Modify

**Core:**
- `src/main/java/org/example/jwtjavaeight/common/Result.java` - add traceId, timestamp
- `src/main/java/org/example/jwtjavaeight/exception/GlobalExceptionHandler.java` - enhance error handling
- `src/main/java/org/example/jwtjavaeight/exception/BusinessException.java` - add httpStatus field

**Controllers:**
- `src/main/java/org/example/jwtjavaeight/controller/AuthController.java` - change paths to `/api/v1/auth/**`, fix logout
- `src/main/java/org/example/jwtjavaeight/controller/UserController.java` - complete CRUD + pagination
- `src/main/java/org/example/jwtjavaeight/controller/RoleController.java` - fix HTTP methods, add pagination
- `src/main/java/org/example/jwtjavaeight/controller/MenuController.java` - add tree endpoint, pagination

**Services:**
- `src/main/java/org/example/jwtjavaeight/service/impl/UserServiceImpl.java`
- `src/main/java/org/example/jwtjavaeight/service/impl/RoleServiceImpl.java`
- `src/main/java/org/example/jwtjavaeight/service/impl/MenuServiceImpl.java`

**Mappers:**
- `src/main/java/org/example/jwtjavaeight/mapper/UserMapper.java` - add pagination methods
- `src/main/java/org/example/jwtjavaeight/mapper/RoleMapper.java` - add pagination methods
- `src/main/java/org/example/jwtjavaeight/mapper/MenuMapper.java` - add tree query
- `src/main/java/org/example/jwtjavaeight/mapper/UserRoleMapper.java` - add batch insert
- `src/main/java/org/example/jwtjavaeight/mapper/RoleMenuMapper.java` - add batch insert

**Mapper XMLs:**
- `src/main/resources/mapper/UserMapper.xml`
- `src/main/resources/mapper/RoleMapper.xml`
- `src/main/resources/mapper/MenuMapper.xml`
- `src/main/resources/mapper/UserRoleMapper.xml`
- `src/main/resources/mapper/RoleMenuMapper.xml`

---

## Stage 1: Foundation Infrastructure (Days 1-3)

### Task 1: Error Code Enum

**Files:**
- Create: `src/main/java/org/example/jwtjavaeight/enums/ErrorCode.java`

- [ ] **Step 1: Create ErrorCode enum**

```java
package org.example.jwtjavaeight.enums;

public enum ErrorCode {
    SUCCESS(200, "操作成功"),
    
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
    
    INTERNAL_ERROR(50000, "服务器内部错误");
    
    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}
```

- [ ] **Step 2: Commit ErrorCode enum**

```bash
git add src/main/java/org/example/jwtjavaeight/enums/ErrorCode.java
git commit -m "feat: add ErrorCode enum with comprehensive error codes

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 2: Data Scope Enum

**Files:**
- Create: `src/main/java/org/example/jwtjavaeight/enums/DataScopeEnum.java`

- [ ] **Step 1: Create DataScopeEnum**

```java
package org.example.jwtjavaeight.enums;

public enum DataScopeEnum {
    ALL("全部数据权限"),
    DEPT("部门数据权限"),
    DEPT_AND_SUB("部门及子部门数据权限"),
    SELF("仅本人数据权限"),
    CUSTOM("自定义数据权限");
    
    private final String description;
    
    DataScopeEnum(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
```

- [ ] **Step 2: Commit DataScopeEnum**

```bash
git add src/main/java/org/example/jwtjavaeight/enums/DataScopeEnum.java
git commit -m "feat: add DataScopeEnum for role data scope

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 3: Menu Type Enum

**Files:**
- Create: `src/main/java/org/example/jwtjavaeight/enums/MenuTypeEnum.java`

- [ ] **Step 1: Create MenuTypeEnum**

```java
package org.example.jwtjavaeight.enums;

public enum MenuTypeEnum {
    DIR(1, "目录"),
    MENU(2, "菜单"),
    BUTTON(3, "按钮");
    
    private final Integer code;
    private final String description;
    
    MenuTypeEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static MenuTypeEnum fromCode(Integer code) {
        for (MenuTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid menu type code: " + code);
    }
}
```

- [ ] **Step 2: Commit MenuTypeEnum**

```bash
git add src/main/java/org/example/jwtjavaeight/enums/MenuTypeEnum.java
git commit -m "feat: add MenuTypeEnum for menu type classification

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 4: Enhance Result Class

**Files:**
- Modify: `src/main/java/org/example/jwtjavaeight/common/Result.java`

- [ ] **Step 1: Read current Result class**

```bash
# Review current implementation
```

- [ ] **Step 2: Add traceId and timestamp fields**

Add to Result.java:
```java
import org.slf4j.MDC;
import java.time.OffsetDateTime;

// Add fields
private String traceId;
private OffsetDateTime timestamp;

// Add to success() method
result.setTraceId(MDC.get("traceId"));
result.setTimestamp(OffsetDateTime.now());

// Add to error() method
result.setTraceId(MDC.get("traceId"));
result.setTimestamp(OffsetDateTime.now());

// Add new error method with ErrorCode
public static <T> Result<T> error(ErrorCode errorCode) {
    Result<T> result = new Result<>();
    result.setCode(errorCode.getCode());
    result.setMessage(errorCode.getMessage());
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
```

- [ ] **Step 3: Commit enhanced Result**

```bash
git add src/main/java/org/example/jwtjavaeight/common/Result.java
git commit -m "feat: enhance Result with traceId and timestamp

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 5: Enhance BusinessException

**Files:**
- Modify: `src/main/java/org/example/jwtjavaeight/exception/BusinessException.java`

- [ ] **Step 1: Add ErrorCode support to BusinessException**

```java
package org.example.jwtjavaeight.exception;

import org.example.jwtjavaeight.enums.ErrorCode;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final int httpStatus;
    private String customMessage;
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getCode() / 100;
    }
    
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getCode() / 100;
        this.customMessage = customMessage;
    }
    
    public static BusinessException of(ErrorCode errorCode, String customMessage) {
        return new BusinessException(errorCode, customMessage);
    }
    
    @Override
    public String getMessage() {
        return customMessage != null ? customMessage : super.getMessage();
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public int getHttpStatus() {
        return httpStatus;
    }
}
```

- [ ] **Step 2: Commit enhanced BusinessException**

```bash
git add src/main/java/org/example/jwtjavaeight/exception/BusinessException.java
git commit -m "feat: enhance BusinessException with ErrorCode and httpStatus

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 6: Create ResourceNotFoundException

**Files:**
- Create: `src/main/java/org/example/jwtjavaeight/exception/ResourceNotFoundException.java`

- [ ] **Step 1: Create ResourceNotFoundException**

```java
package org.example.jwtjavaeight.exception;

import org.example.jwtjavaeight.enums.ErrorCode;

public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String resourceType, Object id) {
        super(ErrorCode.NOT_FOUND, String.format("%s[id=%s]不存在", resourceType, id));
    }
    
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
```

- [ ] **Step 2: Commit ResourceNotFoundException**

```bash
git add src/main/java/org/example/jwtjavaeight/exception/ResourceNotFoundException.java
git commit -m "feat: add ResourceNotFoundException for 404 errors

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 7: Enhance GlobalExceptionHandler

**Files:**
- Modify: `src/main/java/org/example/jwtjavaeight/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Add comprehensive exception handlers**

```java
package org.example.jwtjavaeight.exception;

import org.example.jwtjavaeight.common.Result;
import org.example.jwtjavaeight.enums.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.status(400)
            .body(Result.error(ErrorCode.VALIDATION_FAILED, message));
    }
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
            .body(Result.error(ex.getErrorCode(), ex.getMessage()));
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404)
            .body(Result.error(ex.getErrorCode(), ex.getMessage()));
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(401)
            .body(Result.error(ErrorCode.UNAUTHORIZED, "认证失败"));
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccess(AccessDeniedException ex) {
        return ResponseEntity.status(403)
            .body(Result.error(ErrorCode.FORBIDDEN, "权限不足"));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleGeneric(Exception ex, HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        log.error("Unexpected error [traceId={}]: ", traceId, ex);
        return ResponseEntity.status(500)
            .body(Result.error(ErrorCode.INTERNAL_ERROR, "系统异常，请联系管理员"));
    }
}
```

- [ ] **Step 2: Commit enhanced GlobalExceptionHandler**

```bash
git add src/main/java/org/example/jwtjavaeight/exception/GlobalExceptionHandler.java
git commit -m "feat: enhance GlobalExceptionHandler with comprehensive error handling

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 8: Create PageResponse DTO

**Files:**
- Create: `src/main/java/org/example/jwtjavaeight/domain/dto/PageResponse.java`

- [ ] **Step 1: Create PageResponse**

```java
package org.example.jwtjavaeight.domain.dto;

import java.util.List;

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
    
    // Getters and Setters
    public List<T> getItems() {
        return items;
    }
    
    public void setItems(List<T> items) {
        this.items = items;
    }
    
    public Integer getPage() {
        return page;
    }
    
    public void setPage(Integer page) {
        this.page = page;
    }
    
    public Integer getSize() {
        return size;
    }
    
    public void setSize(Integer size) {
        this.size = size;
    }
    
    public Long getTotal() {
        return total;
    }
    
    public void setTotal(Long total) {
        this.total = total;
    }
    
    public Integer getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }
}
```

- [ ] **Step 2: Commit PageResponse**

```bash
git add src/main/java/org/example/jwtjavaeight/domain/dto/PageResponse.java
git commit -m "feat: add PageResponse DTO for pagination

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 9: Create PageRequest Base Class

**Files:**
- Create: `src/main/java/org/example/jwtjavaeight/domain/dto/PageRequest.java`

- [ ] **Step 1: Create PageRequest**

```java
package org.example.jwtjavaeight.domain.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

public class PageRequest {
    @Min(1)
    private int page = 1;
    
    @Min(1)
    @Max(100)
    private int size = 20;
    
    private String sort = "id";
    private String order = "asc";
    
    public int getOffset() {
        return (page - 1) * size;
    }
    
    // Getters and Setters
    public int getPage() {
        return page;
    }
    
    public void setPage(int page) {
        this.page = page;
    }
    
    public int getSize() {
        return size;
    }
    
    public void setSize(int size) {
        this.size = size;
    }
    
    public String getSort() {
        return sort;
    }
    
    public void setSort(String sort) {
        this.sort = sort;
    }
    
    public String getOrder() {
        return order;
    }
    
    public void setOrder(String order) {
        this.order = order;
    }
}
```

- [ ] **Step 2: Commit PageRequest**

```bash
git add src/main/java/org/example/jwtjavaeight/domain/dto/PageRequest.java
git commit -m "feat: add PageRequest base class for pagination

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 10: Create Query Filter Classes

**Files:**
- Create: `src/main/java/org/example/jwtjavaeight/domain/dto/UserQueryFilter.java`
- Create: `src/main/java/org/example/jwtjavaeight/domain/dto/RoleQueryFilter.java`
- Create: `src/main/java/org/example/jwtjavaeight/domain/dto/MenuQueryFilter.java`

- [ ] **Step 1: Create UserQueryFilter**

```java
package org.example.jwtjavaeight.domain.dto;

public class UserQueryFilter extends PageRequest {
    private String keyword;
    private Integer status;
    private Boolean locked;
    private Long roleId;
    
    // Getters and Setters
    public String getKeyword() {
        return keyword;
    }
    
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public Boolean getLocked() {
        return locked;
    }
    
    public void setLocked(Boolean locked) {
        this.locked = locked;
    }
    
    public Long getRoleId() {
        return roleId;
    }
    
    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
```

- [ ] **Step 2: Create RoleQueryFilter**

```java
package org.example.jwtjavaeight.domain.dto;

public class RoleQueryFilter extends PageRequest {
    private String keyword;
    private Integer level;
    private String dataScope;
    
    // Getters and Setters
    public String getKeyword() {
        return keyword;
    }
    
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    
    public Integer getLevel() {
        return level;
    }
    
    public void setLevel(Integer level) {
        this.level = level;
    }
    
    public String getDataScope() {
        return dataScope;
    }
    
    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }
}
```

- [ ] **Step 3: Create MenuQueryFilter**

```java
package org.example.jwtjavaeight.domain.dto;

public class MenuQueryFilter extends PageRequest {
    private String keyword;
    private Integer menuType;
    private Integer visible;
    private Integer status;
    private Long parentId;
    
    // Getters and Setters
    public String getKeyword() {
        return keyword;
    }
    
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    
    public Integer getMenuType() {
        return menuType;
    }
    
    public void setMenuType(Integer menuType) {
        this.menuType = menuType;
    }
    
    public Integer getVisible() {
        return visible;
    }
    
    public void setVisible(Integer visible) {
        this.visible = visible;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public Long getParentId() {
        return parentId;
    }
    
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
```

- [ ] **Step 4: Commit query filter classes**

```bash
git add src/main/java/org/example/jwtjavaeight/domain/dto/UserQueryFilter.java
git add src/main/java/org/example/jwtjavaeight/domain/dto/RoleQueryFilter.java
git add src/main/java/org/example/jwtjavaeight/domain/dto/MenuQueryFilter.java
git commit -m "feat: add query filter classes for pagination

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 11: Create Role DTOs

**Files:**
- Create: `src/main/java/org/example/jwtjavaeight/domain/dto/RoleCreateRequest.java`
- Create: `src/main/java/org/example/jwtjavaeight/domain/dto/RoleUpdateRequest.java`
- Create: `src/main/java/org/example/jwtjavaeight/domain/dto/RoleResponse.java`

- [ ] **Step 1: Create RoleCreateRequest**

```java
package org.example.jwtjavaeight.domain.dto;

import org.example.jwtjavaeight.enums.DataScopeEnum;

import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class RoleCreateRequest {
    @NotBlank
    @Size(max = 32)
    @Pattern(regexp = "^[A-Z_]{2,32}$")
    private String roleCode;
    
    @NotBlank
    @Size(max = 32)
    private String roleName;
    
    @Min(0)
    @Max(9)
    private Integer level;
    
    @NotNull
    private DataScopeEnum dataScope;
    
    @Size(max = 255)
    private String remark;
    
    // Getters and Setters
    public String getRoleCode() {
        return roleCode;
    }
    
    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }
    
    public String getRoleName() {
        return roleName;
    }
    
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
    
    public Integer getLevel() {
        return level;
    }
    
    public void setLevel(Integer level) {
        this.level = level;
    }
    
    public DataScopeEnum getDataScope() {
        return dataScope;
    }
    
    public void setDataScope(DataScopeEnum dataScope) {
        this.dataScope = dataScope;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
}
```

- [ ] **Step 2: Create RoleUpdateRequest**

```java
package org.example.jwtjavaeight.domain.dto;

import org.example.jwtjavaeight.enums.DataScopeEnum;

import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import javax.validation.constraints.Size;

public class RoleUpdateRequest {
    @Size(max = 32)
    private String roleName;
    
    @Min(0)
    @Max(9)
    private Integer level;
    
    private DataScopeEnum dataScope;
    
    @Size(max = 255)
    private String remark;
    
    // Getters and Setters
    public String getRoleName() {
        return roleName;
    }
    
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
    
    public Integer getLevel() {
        return level;
    }
    
    public void setLevel(Integer level) {
        this.level = level;
    }
    
    public DataScopeEnum getDataScope() {
        return dataScope;
    }
    
    public void setDataScope(DataScopeEnum dataScope) {
        this.dataScope = dataScope;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
}
```

- [ ] **Step 3: Create RoleResponse**

```java
package org.example.jwtjavaeight.domain.dto;

import org.example.jwtjavaeight.enums.DataScopeEnum;

import java.time.OffsetDateTime;

public class RoleResponse {
    private Long id;
    private String roleCode;
    private String roleName;
    private Integer level;
    private DataScopeEnum dataScope;
    private String createBy;
    private OffsetDateTime createTime;
    private String remark;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getRoleCode() {
        return roleCode;
    }
    
    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }
    
    public String getRoleName() {
        return roleName;
    }
    
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
    
    public Integer getLevel() {
        return level;
    }
    
    public void setLevel(Integer level) {
        this.level = level;
    }
    
    public DataScopeEnum getDataScope() {
        return dataScope;
    }
    
    public void setDataScope(DataScopeEnum dataScope) {
        this.dataScope = dataScope;
    }
    
    public String getCreateBy() {
        return createBy;
    }
    
    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
    
    public OffsetDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(OffsetDateTime createTime) {
        this.createTime = createTime;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
}
```

- [ ] **Step 4: Commit Role DTOs**

```bash
git add src/main/java/org/example/jwtjavaeight/domain/dto/RoleCreateRequest.java
git add src/main/java/org/example/jwtjavaeight/domain/dto/RoleUpdateRequest.java
git add src/main/java/org/example/jwtjavaeight/domain/dto/RoleResponse.java
git commit -m "feat: add Role DTOs (CreateRequest, UpdateRequest, Response)

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 12: Create User DTOs

**Files:**
- Create: `src/main/java/org/example/jwtjavaeight/domain/dto/UserCreateRequest.java`
- Create: `src/main/java/org/example/jwtjavaeight/domain/dto/UserUpdateRequest.java`
- Create: `src/main/java/org/example/jwtjavaeight/domain/dto/UserResponse.java`
- Create: `src/main/java/org/example/jwtjavaeight/domain/dto/UserDetailResponse.java`
- Create: `src/main/java/org/example/jwtjavaeight/domain/dto/ChangePasswordRequest.java`
- Create: `src/main/java/org/example/jwtjavaeight/domain/dto/LockUserRequest.java`
- Create: `src/main/java/org/example/jwtjavaeight/domain/dto/ResetPasswordRequest.java`

- [ ] **Step 1: Create UserCreateRequest**

```java
package org.example.jwtjavaeight.domain.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class UserCreateRequest {
    @NotBlank
    @Size(min = 3, max = 20)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    private String username;
    
    @NotBlank
    @Size(min = 6, max = 64)
    private String password;
    
    @Email
    @NotBlank
    private String email;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$")
    private String phone;
    
    @Size(max = 255)
    private String remark;
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
}
```

- [ ] **Step 2: Create UserUpdateRequest**

```java
package org.example.jwtjavaeight.domain.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class UserUpdateRequest {
    @Email
    private String email;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$")
    private String phone;
    
    @Size(max = 255)
    private String remark;
    
    // Getters and Setters
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
}
```

- [ ] **Step 3: Create UserResponse**

```java
package org.example.jwtjavaeight.domain.dto;

import java.time.OffsetDateTime;

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
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public Boolean getLocked() {
        return locked;
    }
    
    public void setLocked(Boolean locked) {
        this.locked = locked;
    }
    
    public String getCreateBy() {
        return createBy;
    }
    
    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
    
    public OffsetDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(OffsetDateTime createTime) {
        this.createTime = createTime;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
}
```

- [ ] **Step 4: Create UserDetailResponse**

```java
package org.example.jwtjavaeight.domain.dto;

import java.util.List;

public class UserDetailResponse extends UserResponse {
    private List<RoleResponse> roles;
    private List<String> permissions;
    private List<MenuTreeNode> menuTree;
    
    // Getters and Setters
    public List<RoleResponse> getRoles() {
        return roles;
    }
    
    public void setRoles(List<RoleResponse> roles) {
        this.roles = roles;
    }
    
    public List<String> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
    
    public List<MenuTreeNode> getMenuTree() {
        return menuTree;
    }
    
    public void setMenuTree(List<MenuTreeNode> menuTree) {
        this.menuTree = menuTree;
    }
}
```

- [ ] **Step 5: Create ChangePasswordRequest**

```java
package org.example.jwtjavaeight.domain.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class ChangePasswordRequest {
    @NotBlank
    private String oldPassword;
    
    @NotBlank
    @Size(min = 6, max = 64)
    private String newPassword;
    
    // Getters and Setters
    public String getOldPassword() {
        return oldPassword;
    }
    
    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }
    
    public String getNewPassword() {
        return newPassword;
    }
    
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
```

- [ ] **Step 6: Create LockUserRequest**

```java
package org.example.jwtjavaeight.domain.dto;

import javax.validation.constraints.Size;

public class LockUserRequest {
    @Size(max = 255)
    private String reason;
    
    // Getters and Setters
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}
```

- [ ] **Step 7: Create ResetPasswordRequest**

```java
package org.example.jwtjavaeight.domain.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class ResetPasswordRequest {
    @NotBlank
    @Size(min = 6, max = 64)
    private String newPassword;
    
    // Getters and Setters
    public String getNewPassword() {
        return newPassword;
    }
    
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
```

- [ ] **Step 8: Commit User DTOs**

```bash
git add src/main/java/org/example/jwtjavaeight/domain/dto/UserCreateRequest.java
git add src/main/java/org/example/jwtjavaeight/domain/dto/UserUpdateRequest.java
git add src/main/java/org/example/jwtjavaeight/domain/dto/UserResponse.java
git add src/main/java/org/example/jwtjavaeight/domain/dto/UserDetailResponse.java
git add src/main/java/org/example/jwtjavaeight/domain/dto/ChangePasswordRequest.java
git add src/main/java/org/example/jwtjavaeight/domain/dto/LockUserRequest.java
git add src/main/java/org/example/jwtjavaeight/domain/dto/ResetPasswordRequest.java
git commit -m "feat: add User DTOs (CreateRequest, UpdateRequest, Response, DetailResponse, etc.)

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 13: Create Menu DTOs

**Files:**
- Create: `src/main/java/org/example/jwtjavaeight/domain/dto/MenuCreateRequest.java`
- Create: `src/main/java/org/example/jwtjavaeight/domain/dto/MenuUpdateRequest.java`
- Create: `src/main/java/org/example/jwtjavaeight/domain/dto/MenuResponse.java`
- Create: `src/main/java/org/example/jwtjavaeight/domain/dto/MenuTreeNode.java`

- [ ] **Step 1: Create MenuCreateRequest**

```java
package org.example.jwtjavaeight.domain.dto;

import org.example.jwtjavaeight.enums.MenuTypeEnum;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class MenuCreateRequest {
    private Long parentId;
    
    @NotBlank
    @Size(max = 32)
    private String menuName;
    
    @NotBlank
    @Size(max = 32)
    @Pattern(regexp = "^[a-z][a-z0-9\\-]*$")
    private String menuCode;
    
    @NotNull
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
    
    // Getters and Setters
    public Long getParentId() {
        return parentId;
    }
    
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
    
    public String getMenuName() {
        return menuName;
    }
    
    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }
    
    public String getMenuCode() {
        return menuCode;
    }
    
    public void setMenuCode(String menuCode) {
        this.menuCode = menuCode;
    }
    
    public MenuTypeEnum getMenuType() {
        return menuType;
    }
    
    public void setMenuType(MenuTypeEnum menuType) {
        this.menuType = menuType;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getComponent() {
        return component;
    }
    
    public void setComponent(String component) {
        this.component = component;
    }
    
    public String getPerms() {
        return perms;
    }
    
    public void setPerms(String perms) {
        this.perms = perms;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public Boolean getVisible() {
        return visible;
    }
    
    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
}
```

- [ ] **Step 2: Create MenuUpdateRequest**

```java
package org.example.jwtjavaeight.domain.dto;

import org.example.jwtjavaeight.enums.MenuTypeEnum;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

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
    
    // Getters and Setters
    public String getMenuName() {
        return menuName;
    }
    
    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }
    
    public MenuTypeEnum getMenuType() {
        return menuType;
    }
    
    public void setMenuType(MenuTypeEnum menuType) {
        this.menuType = menuType;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getComponent() {
        return component;
    }
    
    public void setComponent(String component) {
        this.component = component;
    }
    
    public String getPerms() {
        return perms;
    }
    
    public void setPerms(String perms) {
        this.perms = perms;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public Boolean getVisible() {
        return visible;
    }
    
    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
}
```

- [ ] **Step 3: Create MenuResponse**

```java
package org.example.jwtjavaeight.domain.dto;

import org.example.jwtjavaeight.enums.MenuTypeEnum;

import java.time.OffsetDateTime;

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
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getParentId() {
        return parentId;
    }
    
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
    
    public String getMenuName() {
        return menuName;
    }
    
    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }
    
    public String getMenuCode() {
        return menuCode;
    }
    
    public void setMenuCode(String menuCode) {
        this.menuCode = menuCode;
    }
    
    public MenuTypeEnum getMenuType() {
        return menuType;
    }
    
    public void setMenuType(MenuTypeEnum menuType) {
        this.menuType = menuType;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getComponent() {
        return component;
    }
    
    public void setComponent(String component) {
        this.component = component;
    }
    
    public String getPerms() {
        return perms;
    }
    
    public void setPerms(String perms) {
        this.perms = perms;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public Boolean getVisible() {
        return visible;
    }
    
    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public String getCreateBy() {
        return createBy;
    }
    
    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
    
    public OffsetDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(OffsetDateTime createTime) {
        this.createTime = createTime;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
}
```

- [ ] **Step 4: Create MenuTreeNode**

```java
package org.example.jwtjavaeight.domain.dto;

import java.util.List;

public class MenuTreeNode extends MenuResponse {
    private List<MenuTreeNode> children;
    
    // Getters and Setters
    public List<MenuTreeNode> getChildren() {
        return children;
    }
    
    public void setChildren(List<MenuTreeNode> children) {
        this.children = children;
    }
}
```

- [ ] **Step 5: Commit Menu DTOs**

```bash
git add src/main/java/org/example/jwtjavaeight/domain/dto/MenuCreateRequest.java
git add src/main/java/org/example/jwtjavaeight/domain/dto/MenuUpdateRequest.java
git add src/main/java/org/example/jwtjavaeight/domain/dto/MenuResponse.java
git add src/main/java/org/example/jwtjavaeight/domain/dto/MenuTreeNode.java
git commit -m "feat: add Menu DTOs (CreateRequest, UpdateRequest, Response, TreeNode)

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Stage 1 Complete

- [ ] **Review Stage 1 completion**

All foundation infrastructure is now in place:
- Error code enum
- Data scope and menu type enums
- Enhanced Result class with traceId/timestamp
- Comprehensive exception handling
- Page response and pagination infrastructure
- All DTO classes for User, Role, and Menu

---

## Stage 2-6 Continuation Note

Due to the large scope of this plan (6 stages with 50+ tasks), the complete detailed implementation would exceed reasonable length limits. The pattern established in Stage 1 continues for:

- **Stage 2**: Auth module optimization (logout fix, token rotation, registration enhancement)
- **Stage 3**: User management (complete CRUD, role assignment, lock/unlock, password management)
- **Stage 4**: Role management (pagination, HTTP method fixes, menu assignment, force delete)
- **Stage 5**: Menu management (tree structure, pagination, reverse queries)
- **Stage 6**: Documentation and test data generation

Each stage follows the same TDD pattern:
1. Write failing test
2. Run test to verify failure
3. Implement minimal code
4. Run test to verify pass
5. Commit

For the remaining stages, key implementation priorities are:

1. Update all mapper interfaces and XML files with pagination queries
2. Modify controllers to use new DTOs and `/api/v1/` paths
3. Implement service layer business logic with validation
4. Add comprehensive OpenAPI documentation
5. Generate test data SQL file

Would you like me to:
1. Continue with detailed Stage 2-6 tasks (will be very long)
2. Provide a condensed version with key milestones
3. Focus on a specific stage in detail

