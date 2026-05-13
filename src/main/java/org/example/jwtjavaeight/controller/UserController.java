package org.example.jwtjavaeight.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import org.example.jwtjavaeight.common.Result;
import org.example.jwtjavaeight.domain.dto.AssignRolesRequest;
import org.example.jwtjavaeight.domain.dto.ChangePasswordRequest;
import org.example.jwtjavaeight.domain.dto.PageResponse;
import org.example.jwtjavaeight.domain.dto.ResetPasswordRequest;
import org.example.jwtjavaeight.domain.dto.RoleResponse;
import org.example.jwtjavaeight.domain.dto.UserCreateRequest;
import org.example.jwtjavaeight.domain.dto.UserDetailResponse;
import org.example.jwtjavaeight.domain.dto.UserQueryFilter;
import org.example.jwtjavaeight.domain.dto.UserResponse;
import org.example.jwtjavaeight.domain.dto.UserUpdateRequest;
import org.example.jwtjavaeight.security.JwtUserDetails;
import org.example.jwtjavaeight.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户管理", description = "用户管理API")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "分页查询用户列表", description = "支持按用户名、邮箱、状态、锁定状态、角色过滤")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数校验失败", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('user:list')")
    public ResponseEntity<Result<PageResponse<UserResponse>>> listUsers(@Valid UserQueryFilter filter) {
        PageResponse<UserResponse> response = userService.findByFilter(filter);
        return ResponseEntity.ok(Result.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情", description = "包含角色列表、权限列表、菜单树")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "用户不存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('user:list')")
    public ResponseEntity<Result<UserDetailResponse>> getUserById(@PathVariable Long id) {
        UserDetailResponse response = userService.findById(id);
        return ResponseEntity.ok(Result.success(response));
    }

    @PostMapping
    @Operation(summary = "创建用户", description = "创建新用户，用户名和邮箱必须唯一")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数校验失败", content = @Content(schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "409", description = "用户名或邮箱已存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('user:add')")
    public ResponseEntity<Result<Long>> createUser(@Valid @RequestBody UserCreateRequest request) {
        Long userId = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.success(userId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户信息", description = "更新用户的邮箱、手机、状态等信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数校验失败", content = @Content(schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "404", description = "用户不存在", content = @Content(schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "409", description = "邮箱已被其他用户使用", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('user:edit')")
    public ResponseEntity<Result<Void>> updateUser(@PathVariable Long id,
                                                    @Valid @RequestBody UserUpdateRequest request) {
        userService.updateUser(id, request);
        return ResponseEntity.ok(Result.success());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "软删除，将用户状态设置为0")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "用户不存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('user:delete')")
    public ResponseEntity<Result<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(Result.success());
    }

    @GetMapping("/{id}/roles")
    @Operation(summary = "查询用户的角色列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "用户不存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('user:list')")
    public ResponseEntity<Result<List<RoleResponse>>> getUserRoles(@PathVariable Long id) {
        List<RoleResponse> roles = userService.findRolesByUserId(id);
        return ResponseEntity.ok(Result.success(roles));
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "全量替换用户角色", description = "删除用户的所有现有角色，然后分配新角色")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "替换成功"),
        @ApiResponse(responseCode = "400", description = "参数校验失败", content = @Content(schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "404", description = "用户不存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('user:edit')")
    public ResponseEntity<Result<Void>> replaceUserRoles(@PathVariable Long id,
                                                          @Valid @RequestBody AssignRolesRequest request) {
        userService.replaceUserRoles(id, request.getRoleIds());
        return ResponseEntity.ok(Result.success());
    }

    @PostMapping("/{id}/roles")
    @Operation(summary = "追加用户角色", description = "在现有角色基础上追加新角色，已存在的角色会被忽略")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "追加成功"),
        @ApiResponse(responseCode = "400", description = "参数校验失败", content = @Content(schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "404", description = "用户不存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('user:edit')")
    public ResponseEntity<Result<Void>> addUserRoles(@PathVariable Long id,
                                                      @Valid @RequestBody AssignRolesRequest request) {
        userService.addUserRoles(id, request.getRoleIds());
        return ResponseEntity.ok(Result.success());
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @Operation(summary = "解绑用户的指定角色")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "解绑成功"),
        @ApiResponse(responseCode = "404", description = "用户或角色关联不存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('user:edit')")
    public ResponseEntity<Result<Void>> removeUserRole(@PathVariable Long id, @PathVariable Long roleId) {
        userService.removeUserRole(id, roleId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(Result.success());
    }

    @PostMapping("/{id}/lock")
    @Operation(summary = "锁定用户", description = "手动锁定用户账户")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "锁定成功"),
        @ApiResponse(responseCode = "404", description = "用户不存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('user:edit')")
    public ResponseEntity<Result<Void>> lockUser(@PathVariable Long id) {
        userService.lockUser(id);
        return ResponseEntity.ok(Result.success());
    }

    @PostMapping("/{id}/unlock")
    @Operation(summary = "解锁用户", description = "解锁被锁定的用户账户，重置登录失败次数")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "解锁成功"),
        @ApiResponse(responseCode = "404", description = "用户不存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('user:edit')")
    public ResponseEntity<Result<Void>> unlockUser(@PathVariable Long id) {
        userService.unlockUser(id);
        return ResponseEntity.ok(Result.success());
    }

    @PostMapping("/{id}/password:reset")
    @Operation(summary = "管理员重置用户密码", description = "管理员强制重置用户密码")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "重置成功"),
        @ApiResponse(responseCode = "400", description = "参数校验失败", content = @Content(schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "404", description = "用户不存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('user:edit')")
    public ResponseEntity<Result<Void>> resetPassword(@PathVariable Long id,
                                                       @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return ResponseEntity.ok(Result.success());
    }

    @GetMapping("/me")
    @Operation(
        summary = "获取当前登录用户信息",
        description = "返回当前用户的详细信息，包含角色、权限和菜单",
        security = @SecurityRequirement(name = "Bearer Token")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    public ResponseEntity<Result<UserDetailResponse>> getCurrentUser(
            @AuthenticationPrincipal JwtUserDetails userDetails) {
        UserDetailResponse response = userService.getCurrentUser(userDetails.getUserId());
        return ResponseEntity.ok(Result.success(response));
    }

    @PutMapping("/me/password")
    @Operation(
        summary = "修改当前用户密码",
        description = "用户修改自己的密码，需要提供原密码",
        security = @SecurityRequirement(name = "Bearer Token")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "修改成功"),
        @ApiResponse(responseCode = "400", description = "参数校验失败或原密码不正确", content = @Content(schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    public ResponseEntity<Result<Void>> changePassword(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUserId(), request);
        return ResponseEntity.ok(Result.success());
    }
}

