package org.example.jwtjavaeight.service;

import org.example.jwtjavaeight.domain.dto.MenuCreateRequest;
import org.example.jwtjavaeight.domain.dto.MenuQueryFilter;
import org.example.jwtjavaeight.domain.dto.MenuResponse;
import org.example.jwtjavaeight.domain.dto.MenuTreeNode;
import org.example.jwtjavaeight.domain.dto.MenuUpdateRequest;
import org.example.jwtjavaeight.domain.dto.PageResponse;
import org.example.jwtjavaeight.domain.dto.RoleResponse;

import java.util.List;

public interface MenuService {

    /**
     * 分页查询菜单列表
     */
    PageResponse<MenuResponse> findByFilter(MenuQueryFilter filter);

    /**
     * 根据ID查询菜单（返回DTO）
     */
    MenuResponse findByIdDto(Integer id);

    /**
     * 创建菜单（使用DTO）
     */
    Integer createMenu(MenuCreateRequest request);

    /**
     * 更新菜单（使用DTO）
     */
    void updateMenu(Integer id, MenuUpdateRequest request);

    /**
     * 删除菜单（与deleteById相同，但与Controller保持一致）
     */
    void deleteMenu(Integer id);

    /**
     * 查询完整菜单树
     */
    List<MenuTreeNode> getMenuTree();

    /**
     * 根据用户ID查询菜单树
     */
    List<MenuTreeNode> getMenuTreeByUserId(Long userId);

    /**
     * 反查：拥有该菜单的角色列表
     */
    List<RoleResponse> findRolesByMenuId(Integer menuId);
}
