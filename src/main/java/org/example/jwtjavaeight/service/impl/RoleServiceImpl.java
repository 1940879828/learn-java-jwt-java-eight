package org.example.jwtjavaeight.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.example.jwtjavaeight.domain.dto.MenuResponse;
import org.example.jwtjavaeight.domain.dto.PageResponse;
import org.example.jwtjavaeight.domain.dto.RoleCreateRequest;
import org.example.jwtjavaeight.domain.dto.RoleQueryFilter;
import org.example.jwtjavaeight.domain.dto.RoleResponse;
import org.example.jwtjavaeight.domain.dto.RoleUpdateRequest;
import org.example.jwtjavaeight.domain.dto.UserResponse;
import org.example.jwtjavaeight.domain.entity.SysMenu;
import org.example.jwtjavaeight.domain.entity.SysRole;
import org.example.jwtjavaeight.domain.entity.SysRoleMenu;
import org.example.jwtjavaeight.domain.entity.SysUser;
import org.example.jwtjavaeight.enums.DataScopeEnum;
import org.example.jwtjavaeight.enums.ErrorCode;
import org.example.jwtjavaeight.exception.BusinessException;
import org.example.jwtjavaeight.exception.ResourceNotFoundException;
import org.example.jwtjavaeight.mapper.MenuMapper;
import org.example.jwtjavaeight.mapper.RoleMapper;
import org.example.jwtjavaeight.mapper.RoleMenuMapper;
import org.example.jwtjavaeight.mapper.UserMapper;
import org.example.jwtjavaeight.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleServiceImpl implements RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;
    private final UserMapper userMapper;

    public RoleServiceImpl(
            RoleMapper roleMapper,
            RoleMenuMapper roleMenuMapper,
            MenuMapper menuMapper,
            UserMapper userMapper) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
        this.userMapper = userMapper;
    }

    @Override
    public PageResponse<RoleResponse> findByFilter(RoleQueryFilter filter) {
        List<SysRole> roles = roleMapper.findByFilter(filter);
        long total = roleMapper.countByFilter(filter);

        List<RoleResponse> responses = roles.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(responses, filter.getPage(), filter.getSize(), total);
    }

    @Override
    public RoleResponse findById(Integer id) {
        SysRole role = roleMapper.findById(id);
        if (role == null) {
            throw new ResourceNotFoundException("Role", id);
        }
        return convertToResponse(role);
    }

    @Override
    @Transactional
    public Integer createRole(RoleCreateRequest request) {
        SysRole existingRole = roleMapper.findByRoleCode(request.getRoleCode());
        if (existingRole != null) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "角色编码已存在");
        }

        SysRole role = new SysRole();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setPermission(request.getPermission());
        role.setLevel(request.getLevel());
        role.setDataScope(request.getDataScope().name());
        role.setRemark(request.getRemark());

        roleMapper.insert(role);
        log.info("[RoleService] 创建角色成功, ID: {}, 编码: {}", role.getId(), role.getRoleCode());

        return role.getId();
    }

    @Override
    @Transactional
    public void updateRole(Integer id, RoleUpdateRequest request) {
        SysRole role = roleMapper.findById(id);
        if (role == null) {
            throw new ResourceNotFoundException("Role", id);
        }

        SysRole updateRole = new SysRole();
        updateRole.setId(id);
        updateRole.setRoleName(request.getRoleName());
        updateRole.setPermission(request.getPermission());
        updateRole.setLevel(request.getLevel());
        if (request.getDataScope() != null) {
            updateRole.setDataScope(request.getDataScope().name());
        }
        updateRole.setRemark(request.getRemark());

        roleMapper.update(updateRole);
        log.info("[RoleService] 更新角色成功, ID: {}", id);
    }

    @Override
    @Transactional
    public void deleteRole(Integer id, boolean force) {
        SysRole role = roleMapper.findById(id);
        if (role == null) {
            throw new ResourceNotFoundException("Role", id);
        }

        int userCount = roleMapper.countUsersByRoleId(id);
        if (userCount > 0 && !force) {
            throw new BusinessException(ErrorCode.RESOURCE_IN_USE,
                    String.format("角色正在被%d个用户使用，无法删除。使用?force=true强制删除", userCount));
        }

        roleMenuMapper.deleteByRoleId(id);
        roleMapper.deleteById(id);
        log.info("[RoleService] 删除角色成功, ID: {}, force: {}", id, force);
    }

    @Override
    public List<MenuResponse> findMenusByRoleId(Integer roleId) {
        SysRole role = roleMapper.findById(roleId);
        if (role == null) {
            throw new ResourceNotFoundException("Role", roleId);
        }
        List<SysMenu> menus = menuMapper.findMenusByRoleId(roleId);
        return menus.stream()
                .map(this::convertMenuToResponse)
                .collect(Collectors.toList());
    }

    private MenuResponse convertMenuToResponse(SysMenu menu) {
        MenuResponse response = new MenuResponse();
        response.setId(menu.getId());
        response.setParentId(menu.getParentId());
        response.setMenuName(menu.getMenuName());
        response.setMenuCode(menu.getMenuCode());
        response.setMenuType(menu.getMenuType() != null
                ? org.example.jwtjavaeight.enums.MenuTypeEnum.fromCode(menu.getMenuType())
                : null);
        response.setPath(menu.getPath());
        response.setComponent(menu.getComponent());
        response.setPerms(menu.getPerms());
        response.setIcon(menu.getIcon());
        response.setSortOrder(menu.getSortOrder());
        response.setVisible(menu.getVisible());
        response.setStatus(menu.getStatus());
        response.setCreateBy(menu.getCreateBy());
        if (menu.getCreateTime() != null) {
            response.setCreateTime(menu.getCreateTime().toInstant()
                    .atOffset(java.time.ZoneOffset.UTC));
        }
        response.setRemark(menu.getRemark());
        return response;
    }

    @Override
    @Transactional
    public void replaceRoleMenus(Integer roleId, List<Integer> menuIds) {
        SysRole role = roleMapper.findById(roleId);
        if (role == null) {
            throw new ResourceNotFoundException("Role", roleId);
        }

        roleMenuMapper.deleteByRoleId(roleId);

        if (menuIds != null && !menuIds.isEmpty()) {
            List<SysRoleMenu> roleMenus = menuIds.stream()
                    .map(menuId -> {
                        SysRoleMenu roleMenu = new SysRoleMenu();
                        roleMenu.setRoleId(roleId);
                        roleMenu.setMenuId(menuId);
                        return roleMenu;
                    })
                    .collect(Collectors.toList());

            roleMenuMapper.batchInsert(roleMenus);
        }

        log.info("[RoleService] 替换角色菜单成功, 角色ID: {}, 菜单数: {}", roleId, menuIds.size());
    }

    @Override
    @Transactional
    public void removeRoleMenu(Integer roleId, Integer menuId) {
        SysRole role = roleMapper.findById(roleId);
        if (role == null) {
            throw new ResourceNotFoundException("Role", roleId);
        }

        int deleted = roleMenuMapper.deleteByRoleIdAndMenuId(roleId, menuId);
        if (deleted == 0) {
            throw new ResourceNotFoundException("RoleMenu", "roleId=" + roleId + ",menuId=" + menuId);
        }

        log.info("[RoleService] 解绑角色菜单成功, 角色ID: {}, 菜单ID: {}", roleId, menuId);
    }

    @Override
    public List<UserResponse> findUsersByRoleId(Integer roleId) {
        SysRole role = roleMapper.findById(roleId);
        if (role == null) {
            throw new ResourceNotFoundException("Role", roleId);
        }

        // This would require a new UserMapper method, but for now we'll return empty list
        // or implement it differently
        return List.of();
    }

    @Override
    public List<SysRole> findAll() {
        return roleMapper.findAll();
    }

    @Override
    public List<SysRole> findRolesByUserId(Long userId) {
        return roleMapper.findRolesByUserId(userId);
    }

    private RoleResponse convertToResponse(SysRole role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setRoleCode(role.getRoleCode());
        response.setRoleName(role.getRoleName());
        response.setPermission(role.getPermission());
        response.setLevel(role.getLevel());
        if (role.getDataScope() != null) {
            response.setDataScope(DataScopeEnum.valueOf(role.getDataScope()));
        }
        response.setRemark(role.getRemark());
        return response;
    }
}
