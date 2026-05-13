package org.example.jwtjavaeight.service;

import java.util.List;
import org.example.jwtjavaeight.domain.dto.PageResponse;
import org.example.jwtjavaeight.domain.dto.RoleCreateRequest;
import org.example.jwtjavaeight.domain.dto.RoleQueryFilter;
import org.example.jwtjavaeight.domain.dto.RoleResponse;
import org.example.jwtjavaeight.domain.dto.RoleUpdateRequest;
import org.example.jwtjavaeight.domain.dto.UserResponse;
import org.example.jwtjavaeight.domain.entity.SysMenu;
import org.example.jwtjavaeight.domain.entity.SysRole;

public interface RoleService {
    /**
     * 分页查询角色列表
     */
    PageResponse<RoleResponse> findByFilter(RoleQueryFilter filter);

    /**
     * 根据ID查询角色详情
     */
    RoleResponse findById(Integer id);

    /**
     * 创建角色
     */
    Integer createRole(RoleCreateRequest request);

    /**
     * 更新角色
     */
    void updateRole(Integer id, RoleUpdateRequest request);

    /**
     * 删除角色（支持强制删除）
     */
    void deleteRole(Integer id, boolean force);

    /**
     * 查询角色的菜单列表
     */
    List<SysMenu> findMenusByRoleId(Integer roleId);

    /**
     * 全量替换角色菜单
     */
    void replaceRoleMenus(Integer roleId, List<Integer> menuIds);

    /**
     * 解绑单个菜单
     */
    void removeRoleMenu(Integer roleId, Integer menuId);

    /**
     * 反查：拥有该角色的用户列表
     */
    List<UserResponse> findUsersByRoleId(Integer roleId);

    /**
     * 查询所有角色（向后兼容）
     */
    List<SysRole> findAll();

    /**
     * 根据用户ID查询角色列表（向后兼容）
     */
    List<SysRole> findRolesByUserId(Long userId);
}
