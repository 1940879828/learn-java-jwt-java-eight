package org.example.jwtjavaeight.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import org.example.jwtjavaeight.common.Result;
import org.example.jwtjavaeight.domain.dto.AssignMenusRequest;
import org.example.jwtjavaeight.domain.dto.MenuResponse;
import org.example.jwtjavaeight.domain.dto.PageResponse;
import org.example.jwtjavaeight.domain.dto.RoleCreateRequest;
import org.example.jwtjavaeight.domain.dto.RoleQueryFilter;
import org.example.jwtjavaeight.domain.dto.RoleResponse;
import org.example.jwtjavaeight.domain.dto.RoleUpdateRequest;
import org.example.jwtjavaeight.domain.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.example.jwtjavaeight.service.RoleService;

@Tag(name = "角色管理", description = "角色管理API")
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @Operation(summary = "分页查询角色列表", description = "支持按角色编码、名称、级别过滤")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数校验失败", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('role:list')")
    public ResponseEntity<Result<PageResponse<RoleResponse>>> listRoles(@Valid RoleQueryFilter filter) {
        PageResponse<RoleResponse> response = roleService.findByFilter(filter);
        return ResponseEntity.ok(Result.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询角色详情")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "角色不存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('role:list')")
    public ResponseEntity<Result<RoleResponse>> getRoleById(@PathVariable Integer id) {
        RoleResponse response = roleService.findById(id);
        return ResponseEntity.ok(Result.success(response));
    }

    @PostMapping
    @Operation(summary = "创建角色", description = "创建新角色，角色编码必须唯一")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数校验失败", content = @Content(schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "409", description = "角色编码已存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('role:add')")
    public ResponseEntity<Result<Integer>> createRole(@Valid @RequestBody RoleCreateRequest request) {
        Integer roleId = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.success(roleId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新角色信息", description = "更新角色的名称、权限、级别、数据权限等信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数校验失败", content = @Content(schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "404", description = "角色不存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('role:edit')")
    public ResponseEntity<Result<Void>> updateRole(@PathVariable Integer id,
                                                    @Valid @RequestBody RoleUpdateRequest request) {
        roleService.updateRole(id, request);
        return ResponseEntity.ok(Result.success());
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除角色",
        description = "删除角色。如果角色被用户使用，默认无法删除，需要设置force=true强制删除"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "角色不存在", content = @Content(schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "409", description = "角色被引用，无法删除", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('role:delete')")
    public ResponseEntity<Result<Void>> deleteRole(
            @PathVariable Integer id,
            @Parameter(description = "是否强制删除") @RequestParam(defaultValue = "false") boolean force) {
        roleService.deleteRole(id, force);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(Result.success());
    }

    @GetMapping("/{id}/menus")
    @Operation(summary = "查询角色的菜单列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "角色不存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('role:list')")
    public ResponseEntity<Result<List<MenuResponse>>> getRoleMenus(@PathVariable Integer id) {
        List<MenuResponse> menus = roleService.findMenusByRoleId(id);
        return ResponseEntity.ok(Result.success(menus));
    }

    @PutMapping("/{id}/menus")
    @Operation(summary = "全量替换角色菜单", description = "删除角色的所有现有菜单，然后分配新菜单")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "替换成功"),
        @ApiResponse(responseCode = "400", description = "参数校验失败", content = @Content(schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "404", description = "角色不存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('role:edit')")
    public ResponseEntity<Result<Void>> replaceRoleMenus(@PathVariable Integer id,
                                                          @Valid @RequestBody AssignMenusRequest request) {
        roleService.replaceRoleMenus(id, request.getMenuIds());
        return ResponseEntity.ok(Result.success());
    }

    @DeleteMapping("/{id}/menus/{menuId}")
    @Operation(summary = "解绑角色的指定菜单")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "解绑成功"),
        @ApiResponse(responseCode = "404", description = "角色或菜单关联不存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('role:edit')")
    public ResponseEntity<Result<Void>> removeRoleMenu(@PathVariable Integer id, @PathVariable Integer menuId) {
        roleService.removeRoleMenu(id, menuId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(Result.success());
    }

    @GetMapping("/{id}/users")
    @Operation(summary = "反查：拥有此角色的用户列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "角色不存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('role:list')")
    public ResponseEntity<Result<List<UserResponse>>> getRoleUsers(@PathVariable Integer id) {
        List<UserResponse> users = roleService.findUsersByRoleId(id);
        return ResponseEntity.ok(Result.success(users));
    }
}
