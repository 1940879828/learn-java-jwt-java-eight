# Timestamp Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace every server-returned `OffsetDateTime` time field with `Long` (Unix epoch in **seconds**, 10-digit), pushing timezone formatting to the frontend.

**Architecture:** Entity layer keeps `java.util.Date` (MyBatis-friendly, no DB changes). Response DTOs change `OffsetDateTime → Long`. A new `DateUtils` helper centralizes the `Date → Long` conversion so every call site reads the same way: `DateUtils.toEpochSeconds(entity.getCreateTime())`. The `Result` envelope's `timestamp` also switches to seconds.

**Tech Stack:** Java 8, Spring Boot 2.7.6, MyBatis. No new dependencies.

**Project note:** Per `CLAUDE.md`, do **NOT** run `mvn compile`, `mvn clean package`, or `mvn spring-boot:run` automatically. Compile/run verification is the user's responsibility. The plan's "verification" steps are code-review-based.

---

## Scope Summary

**In scope** (user chose "响应 + 请求 DTO" + `Long` + 10-digit seconds):

| Layer | Change |
|---|---|
| `org.example.jwtjavaeight.utils.DateUtils` (new) | Centralized `Date → Long` helper |
| `UserResponse.createTime` | `OffsetDateTime → Long` |
| `MenuResponse.createTime` | `OffsetDateTime → Long` |
| `RoleResponse.createTime` | `OffsetDateTime → Long` |
| `MenuTreeNode` | Inherits from `MenuResponse` — no change needed |
| `UserDetailResponse` | Inherits from `UserResponse` — no change needed |
| `Result.timestamp` | `OffsetDateTime → Long`; `OffsetDateTime.now()` → `System.currentTimeMillis() / 1000` |
| `MenuServiceImpl` | 3 conversion sites (lines 246-247, 269-270, 307-308) |
| `RoleServiceImpl` | 1 conversion site (lines 160-161) |

**Not in scope** (verified during exploration):
- **Request DTOs**: Current codebase has *no* time fields in any request/query DTO (`UserQueryFilter`, `MenuQueryFilter`, `RoleQueryFilter`, `UserCreateRequest`, `MenuCreateRequest`, `RoleCreateRequest`, etc.). Migration is moot. *Convention going forward*: any future time input should also use `Long` epoch seconds.
- **Entity layer** (`SysUser`, `SysMenu`, `SysRole`, `SysLoginLog`, `SysRefreshToken`, `SysUserRole`, `SysRoleMenu`): keep `java.util.Date` because MyBatis maps MySQL `DATETIME` to `Date` by default. Changing these would require custom `TypeHandler`s or schema changes — explicitly excluded by the user choice.
- **Internal `Date` usage**: `new Date()` calls in `AuthServiceImpl`, `UserServiceImpl`, `LoginSuccessHandler`, `LoginFailureHandler` write into entity fields, never serialized — leave untouched.
- **`LoginFailureData.lockRemainingSeconds`**: already `Long` representing a duration (not a timestamp). No change.

## Discovered Pre-Existing Issues (informational — NOT part of this migration)

These exist *before* the migration and would persist after. Decide separately whether to fix:

1. `UserServiceImpl.convertToResponse` (`UserServiceImpl.java:331-341`) never calls `setCreateTime` or `setCreateBy`. `UserResponse.createTime` is always `null` today; this plan preserves that. After this migration the field will simply be a `null Long` instead of a `null OffsetDateTime`.
2. `RoleServiceImpl.convertToResponse` (`RoleServiceImpl.java:233-245`) never sets `createTime` / `createBy` either.
3. `UserServiceImpl.convertRoleToResponse` (`UserServiceImpl.java:180-192`) never sets `createTime` either.

If you want these gaps fixed too, add a follow-up task that simply inserts `response.setCreateTime(DateUtils.toEpochSeconds(entity.getCreateTime()))` into each — trivial once `DateUtils` exists.

---

## Frontend Contract (informational, share with FE team)

Before this change:
```json
{
  "code": 200,
  "timestamp": "2026-05-15T10:30:00Z",
  "data": { "createTime": "2026-05-15T08:00:00Z" }
}
```

After this change:
```json
{
  "code": 200,
  "timestamp": 1747304400,
  "data": { "createTime": 1747296000 }
}
```

Frontend conversion (JavaScript):
```javascript
new Date(createTime * 1000)
```

---

### Task 1: Create `DateUtils` helper

**Files:**
- Create: `src/main/java/org/example/jwtjavaeight/utils/DateUtils.java`

- [ ] **Step 1: Create the helper class**

```java
package org.example.jwtjavaeight.utils;

import java.util.Date;

/**
 * 时间转换工具：服务端统一返回 Unix 秒级时间戳，时区由前端处理。
 */
public final class DateUtils {

    private DateUtils() {
    }

    /**
     * 将 java.util.Date 转换为 Unix 秒级时间戳（10 位）。
     * 入参为 null 时返回 null。
     */
    public static Long toEpochSeconds(Date date) {
        return date == null ? null : date.getTime() / 1000L;
    }

    /**
     * 当前时间的 Unix 秒级时间戳。
     */
    public static Long nowEpochSeconds() {
        return System.currentTimeMillis() / 1000L;
    }
}
```

- [ ] **Step 2: Verify by reading the file**

Open the file and confirm: package matches existing `org.example.jwtjavaeight.utils` (sibling to `JwtUtil`, `HashUtil`), no Lombok annotations needed (it's a static utility), private constructor prevents instantiation.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/org/example/jwtjavaeight/utils/DateUtils.java
git commit -m "feat: 新增 DateUtils 时间戳转换工具"
```

---

### Task 2: Update `UserResponse` DTO

**Files:**
- Modify: `src/main/java/org/example/jwtjavaeight/domain/dto/UserResponse.java:6,20`

- [ ] **Step 1: Replace the import**

Change line 6:
```java
import java.time.OffsetDateTime;
```
to: remove this line entirely (no new import needed — `Long` is in `java.lang`).

- [ ] **Step 2: Change the field type**

Change line 20:
```java
private OffsetDateTime createTime;
```
to:
```java
private Long createTime;
```

- [ ] **Step 3: Verify by reading the diff**

`git diff src/main/java/org/example/jwtjavaeight/domain/dto/UserResponse.java` — should show one import removed and one field type changed. No other lines should differ. `UserDetailResponse` inherits from this class, so it automatically picks up the new type — no separate change needed.

- [ ] **Step 4: Commit (deferred — bundle with Tasks 3 and 4)**

Do not commit yet. DTOs are tightly coupled to service-impl conversions; bundling all DTO + conversion changes into one logical commit avoids leaving a broken compile state in history.

---

### Task 3: Update `MenuResponse` DTO

**Files:**
- Modify: `src/main/java/org/example/jwtjavaeight/domain/dto/MenuResponse.java:7,26`

- [ ] **Step 1: Remove the import**

Delete line 7:
```java
import java.time.OffsetDateTime;
```

- [ ] **Step 2: Change the field type**

Change line 26:
```java
private OffsetDateTime createTime;
```
to:
```java
private Long createTime;
```

- [ ] **Step 3: Verify `MenuTreeNode` needs nothing**

Open `src/main/java/org/example/jwtjavaeight/domain/dto/MenuTreeNode.java` and confirm it `extends MenuResponse` and has no local `createTime` field. No change needed there.

- [ ] **Step 4: Commit (deferred — bundle with Tasks 2 and 4)**

---

### Task 4: Update `RoleResponse` DTO

**Files:**
- Modify: `src/main/java/org/example/jwtjavaeight/domain/dto/RoleResponse.java:7,20`

- [ ] **Step 1: Remove the import**

Delete line 7:
```java
import java.time.OffsetDateTime;
```

- [ ] **Step 2: Change the field type**

Change line 20:
```java
private OffsetDateTime createTime;
```
to:
```java
private Long createTime;
```

- [ ] **Step 3: Commit DTOs together**

```bash
git add src/main/java/org/example/jwtjavaeight/domain/dto/UserResponse.java \
        src/main/java/org/example/jwtjavaeight/domain/dto/MenuResponse.java \
        src/main/java/org/example/jwtjavaeight/domain/dto/RoleResponse.java
git commit -m "refactor: 响应 DTO 的 createTime 改为 Long 秒级时间戳"
```

Note: After this commit, the project will *not compile* until Tasks 5–7 are done — service impl conversions still call `setCreateTime(OffsetDateTime)`. Do NOT run `mvn compile` between tasks. Move directly to Task 5.

---

### Task 5: Update `MenuServiceImpl` conversion sites

**Files:**
- Modify: `src/main/java/org/example/jwtjavaeight/service/impl/MenuServiceImpl.java:245-248, 268-271, 306-309`

This impl has **three** conversion sites that all currently use `.toInstant().atOffset(java.time.ZoneOffset.UTC)`. Replace each with a `DateUtils.toEpochSeconds(...)` call.

- [ ] **Step 1: Add the import**

Near the top of the file (after the existing `import` block ending around line 24), add:
```java
import org.example.jwtjavaeight.utils.DateUtils;
```

- [ ] **Step 2: Replace site #1 — `convertToRoleResponse` (lines 245-248)**

Current:
```java
        response.setCreateBy(role.getCreateBy());
        if (role.getCreateTime() != null) {
            response.setCreateTime(role.getCreateTime().toInstant()
                    .atOffset(java.time.ZoneOffset.UTC));
        }
```

Replace with:
```java
        response.setCreateBy(role.getCreateBy());
        response.setCreateTime(DateUtils.toEpochSeconds(role.getCreateTime()));
```

(The null check is no longer needed — `DateUtils.toEpochSeconds` returns `null` when input is `null`, and `setCreateTime(null)` is harmless.)

- [ ] **Step 3: Replace site #2 — `convertToResponse` (lines 268-271)**

Current:
```java
        response.setCreateBy(menu.getCreateBy());
        if (menu.getCreateTime() != null) {
            response.setCreateTime(menu.getCreateTime().toInstant()
                    .atOffset(java.time.ZoneOffset.UTC));
        }
        response.setRemark(menu.getRemark());
```

Replace with:
```java
        response.setCreateBy(menu.getCreateBy());
        response.setCreateTime(DateUtils.toEpochSeconds(menu.getCreateTime()));
        response.setRemark(menu.getRemark());
```

- [ ] **Step 4: Replace site #3 — `buildMenuTree` (lines 305-309)**

Current:
```java
                node.setCreateBy(menu.getCreateBy());
                if (menu.getCreateTime() != null) {
                    node.setCreateTime(menu.getCreateTime().toInstant()
                            .atOffset(java.time.ZoneOffset.UTC));
                }
                node.setRemark(menu.getRemark());
```

Replace with:
```java
                node.setCreateBy(menu.getCreateBy());
                node.setCreateTime(DateUtils.toEpochSeconds(menu.getCreateTime()));
                node.setRemark(menu.getRemark());
```

- [ ] **Step 5: Verify by reading the diff**

`git diff src/main/java/org/example/jwtjavaeight/service/impl/MenuServiceImpl.java` should show:
- One added import (`DateUtils`)
- Three blocks shortened from 4 lines to 1 line each
- No other changes

- [ ] **Step 6: Commit (deferred — bundle with Task 6)**

---

### Task 6: Update `RoleServiceImpl` conversion site

**Files:**
- Modify: `src/main/java/org/example/jwtjavaeight/service/impl/RoleServiceImpl.java:159-162`

This impl has **one** conversion site (`convertMenuToResponse`). The other method `convertToResponse(SysRole role)` does not currently set `createTime` — that's a pre-existing gap (see "Discovered Pre-Existing Issues" at the top of this plan) and is NOT changed here.

- [ ] **Step 1: Add the import**

After the existing `import` block (around lines 1-26), add:
```java
import org.example.jwtjavaeight.utils.DateUtils;
```

- [ ] **Step 2: Replace the conversion**

Current (lines 158-163):
```java
        response.setCreateBy(menu.getCreateBy());
        if (menu.getCreateTime() != null) {
            response.setCreateTime(menu.getCreateTime().toInstant()
                    .atOffset(java.time.ZoneOffset.UTC));
        }
        response.setRemark(menu.getRemark());
```

Replace with:
```java
        response.setCreateBy(menu.getCreateBy());
        response.setCreateTime(DateUtils.toEpochSeconds(menu.getCreateTime()));
        response.setRemark(menu.getRemark());
```

- [ ] **Step 3: Commit service impls together**

```bash
git add src/main/java/org/example/jwtjavaeight/service/impl/MenuServiceImpl.java \
        src/main/java/org/example/jwtjavaeight/service/impl/RoleServiceImpl.java
git commit -m "refactor: 服务层转换使用 DateUtils 输出秒级时间戳"
```

---

### Task 7: Update `Result` envelope

**Files:**
- Modify: `src/main/java/org/example/jwtjavaeight/common/Result.java`

This is a separate commit because it affects every API response, not just DTOs with `createTime`.

- [ ] **Step 1: Swap imports**

Remove line 7:
```java
import java.time.OffsetDateTime;
```

Add (in the same alphabetical position among `org.example` imports — between lines 3 and 5):
```java
import org.example.jwtjavaeight.utils.DateUtils;
```

- [ ] **Step 2: Change the field type**

Change line 25:
```java
  private OffsetDateTime timestamp;
```
to:
```java
  private Long timestamp;
```

- [ ] **Step 3: Replace all five `OffsetDateTime.now()` calls**

Find every occurrence of:
```java
    result.setTimestamp(OffsetDateTime.now());
```
(there are 5 — in `success(T)`, `success()`, `failure(Integer, String)`, `failure(Integer, String, T)`, `error(ErrorCode, String)`)

Replace each with:
```java
    result.setTimestamp(DateUtils.nowEpochSeconds());
```

- [ ] **Step 4: Verify by reading the diff**

`git diff src/main/java/org/example/jwtjavaeight/common/Result.java` should show:
- One import removed, one added
- One field type change
- Five identical method-body line replacements

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/example/jwtjavaeight/common/Result.java
git commit -m "refactor: Result.timestamp 改为 Long 秒级时间戳"
```

---

### Task 8: Final verification (user runs)

This task is **for the user to run**, not the agent. Per `CLAUDE.md`, the agent must not run `mvn` commands automatically.

- [ ] **Step 1: Compile**

The user runs:
```bash
mvn compile
```
Expected: BUILD SUCCESS. If failures appear, they should reference unchanged files only (likely no failures — the four touched call paths are exhaustive per the grep audit in this plan).

- [ ] **Step 2: Existing tests**

The user runs:
```bash
mvn test
```
Expected: All tests pass. The grep audit confirmed no existing test references `createTime`, `OffsetDateTime`, or `timestamp` fields, so this should not regress.

- [ ] **Step 3: Manual API smoke test**

The user starts the app (`mvn spring-boot:run`) and hits any endpoint, e.g.:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"..."}'
```

Expected response shape:
```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "traceId": "...",
  "timestamp": 1747304400
}
```

`timestamp` should be a 10-digit integer, not an ISO string.

Then a query that exposes `createTime`:
```bash
curl http://localhost:8080/api/menus -H "Authorization: Bearer <token>"
```

Each menu's `createTime` should be a 10-digit integer (or `null`).

---

## Post-Migration Notes

1. **Frontend impact**: All clients consuming these endpoints must change their date parsing. The previous ISO string `"2026-05-15T10:30:00Z"` was directly accepted by `new Date(str)`; the new `1747296000` requires `new Date(value * 1000)`. **Coordinate the deploy with the frontend team.**

2. **Swagger / OpenAPI**: Schema docs auto-regenerate from the Java types, so `createTime` and `timestamp` will display as `integer (int64)` instead of `string (date-time)` after restart. No manual doc update needed.

3. **Time-zone consistency**: With the migration, the server no longer makes any timezone assertion in the wire format — the timestamp is an absolute instant. The pre-existing concern from the previous `OffsetDateTime` design (whether JVM timezone matches DB) is now invisible at the API boundary, but the same logical question remains: *what timezone does MyBatis use to interpret the MySQL `DATETIME` column?* This plan does NOT fix that. If you want a permanent fix, set `serverTimezone=UTC` in the JDBC URL and start the JVM with `-Duser.timezone=UTC`. That can be a follow-up commit.

4. **Future request inputs**: If any future API accepts time as input (e.g., `startTime`/`endTime` filters), declare it as `Long` epoch seconds. Document this in the API conventions.
