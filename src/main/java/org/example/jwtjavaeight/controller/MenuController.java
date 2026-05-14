package org.example.jwtjavaeight.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.jwtjavaeight.common.Result;
import org.example.jwtjavaeight.domain.dto.MenuCreateRequest;
import org.example.jwtjavaeight.domain.dto.MenuQueryFilter;
import org.example.jwtjavaeight.domain.dto.MenuResponse;
import org.example.jwtjavaeight.domain.dto.MenuTreeNode;
import org.example.jwtjavaeight.domain.dto.MenuUpdateRequest;
import org.example.jwtjavaeight.domain.dto.PageResponse;
import org.example.jwtjavaeight.domain.dto.RoleResponse;
import org.example.jwtjavaeight.service.MenuService;
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
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "菜单管理", description = "菜单权限管理API")
@RestController
@RequestMapping("/api/v1/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    @Operation(summary = "分页查询菜单列表", description = "支持按菜单名称、编码、类型过滤")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数校验失败", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('menu:list')")
    public ResponseEntity<Result<PageResponse<MenuResponse>>> listMenus(@Valid MenuQueryFilter filter) {
        log.info("[MenuController] 分页查询菜单列表, filter: {}", filter);
        PageResponse<MenuResponse> response = menuService.findByFilter(filter);
        log.info("[MenuController] 查询完成, 返回 {} 条记录", response.getItems().size());
        return ResponseEntity.ok(Result.success(response));
    }

    @GetMapping("/tree")
    @Operation(summary = "查询菜单树形结构", description = "返回完整的菜单树，不分页")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @PreAuthorize("hasAuthority('menu:list')")
    public ResponseEntity<Result<List<MenuTreeNode>>> getMenuTree() {
        log.info("[MenuController] 查询完整菜单树");
        List<MenuTreeNode> tree = menuService.getMenuTree();
        log.info("[MenuController] 菜单树查询完成, 根节点数量: {}", tree.size());
        return ResponseEntity.ok(Result.success(tree));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询菜单详情")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "菜单不存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('menu:list')")
    public ResponseEntity<Result<MenuResponse>> getMenuById(@PathVariable Integer id) {
        log.info("[MenuController] 查询菜单详情, id: {}", id);
        MenuResponse response = menuService.findByIdDto(id);
        log.info("[MenuController] 菜单详情查询成功: {}", response.getMenuName());
        return ResponseEntity.ok(Result.success(response));
    }

    @PostMapping
    @Operation(summary = "创建菜单", description = "创建新菜单，菜单编码必须唯一")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数校验失败", content = @Content(schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "409", description = "菜单编码已存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('menu:add')")
    public ResponseEntity<Result<Integer>> createMenu(@Valid @RequestBody MenuCreateRequest request) {
        log.info("[MenuController] 创建菜单, menuName: {}, menuCode: {}, menuType: {}",
            request.getMenuName(), request.getMenuCode(), request.getMenuType());
        Integer menuId = menuService.createMenu(request);
        log.info("[MenuController] 菜单创建成功, id: {}", menuId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.success(menuId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新菜单信息", description = "更新菜单的名称、类型、路径、权限标识等信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数校验失败", content = @Content(schema = @Schema(implementation = Result.class))),
        @ApiResponse(responseCode = "404", description = "菜单不存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('menu:edit')")
    public ResponseEntity<Result<Void>> updateMenu(@PathVariable Integer id,
                                                    @Valid @RequestBody MenuUpdateRequest request) {
        log.info("[MenuController] 更新菜单, id: {}, menuName: {}", id, request.getMenuName());
        menuService.updateMenu(id, request);
        log.info("[MenuController] 菜单更新成功, id: {}", id);
        return ResponseEntity.ok(Result.success());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜单", description = "删除菜单")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "菜单不存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('menu:delete')")
    public ResponseEntity<Result<Void>> deleteMenu(@PathVariable Integer id) {
        log.info("[MenuController] 删除菜单, id: {}", id);
        menuService.deleteMenu(id);
        log.info("[MenuController] 菜单删除成功, id: {}", id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(Result.success());
    }

    @GetMapping("/{id}/roles")
    @Operation(summary = "反查：拥有此菜单的角色列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "菜单不存在", content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PreAuthorize("hasAuthority('menu:list')")
    public ResponseEntity<Result<List<RoleResponse>>> getMenuRoles(@PathVariable Integer id) {
        log.info("[MenuController] 查询菜单的角色列表, menuId: {}", id);
        List<RoleResponse> roles = menuService.findRolesByMenuId(id);
        log.info("[MenuController] 查询到 {} 个角色", roles.size());
        return ResponseEntity.ok(Result.success(roles));
    }
}
