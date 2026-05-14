package org.example.jwtjavaeight.service.impl;

import org.example.jwtjavaeight.domain.dto.MenuCreateRequest;
import org.example.jwtjavaeight.domain.dto.MenuQueryFilter;
import org.example.jwtjavaeight.domain.dto.MenuResponse;
import org.example.jwtjavaeight.domain.dto.MenuTreeNode;
import org.example.jwtjavaeight.domain.dto.MenuUpdateRequest;
import org.example.jwtjavaeight.domain.dto.PageResponse;
import org.example.jwtjavaeight.domain.dto.RoleResponse;
import org.example.jwtjavaeight.domain.entity.SysMenu;
import org.example.jwtjavaeight.exception.ResourceNotFoundException;
import org.example.jwtjavaeight.mapper.MenuMapper;
import org.example.jwtjavaeight.service.MenuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl implements MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuServiceImpl.class);

    @Autowired
    private MenuMapper menuMapper;

    @Override
    public List<SysMenu> findAll() {
        log.info("[MenuService] 查询所有菜单");
        List<SysMenu> menus = menuMapper.findAll();
        log.info("[MenuService] 查询到 {} 条菜单记录", menus.size());
        return menus;
    }

    @Override
    public SysMenu findById(Integer id) {
        log.info("[MenuService] 根据ID查询菜单, id: {}", id);
        SysMenu menu = menuMapper.findById(id);
        if (menu != null) {
            log.info("[MenuService] 查询到菜单: {}", menu.getMenuName());
        } else {
            log.warn("[MenuService] 菜单不存在, id: {}", id);
        }
        return menu;
    }

    @Override
    public List<SysMenu> findMenusByRoleId(Integer roleId) {
        log.info("[MenuService] 根据角色ID查询菜单, roleId: {}", roleId);
        List<SysMenu> menus = menuMapper.findMenusByRoleId(roleId);
        log.info("[MenuService] 角色拥有 {} 个菜单权限", menus.size());
        return menus;
    }

    @Override
    public List<SysMenu> findMenusByUserId(Long userId) {
        log.info("[MenuService] 根据用户ID查询菜单, userId: {}", userId);
        List<SysMenu> menus = menuMapper.findMenusByUserId(userId);
        log.info("[MenuService] 用户拥有 {} 个菜单权限", menus.size());
        return menus;
    }

    @Override
    @Transactional
    public SysMenu create(SysMenu menu) {
        menuMapper.insert(menu);
        return menu;
    }

    @Override
    @Transactional
    public SysMenu update(SysMenu menu) {
        menuMapper.update(menu);
        return menuMapper.findById(menu.getId());
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        menuMapper.deleteById(id);
    }

    @Override
    public PageResponse<MenuResponse> findByFilter(MenuQueryFilter filter) {
        log.info("[MenuService] 分页查询菜单, page: {}, size: {}, keyword: {}",
            filter.getPage(), filter.getSize(), filter.getKeyword());

        List<SysMenu> menus = menuMapper.findByFilter(filter);
        long total = menuMapper.countByFilter(filter);

        log.info("[MenuService] 查询结果: 当前页 {} 条, 总计 {} 条", menus.size(), total);

        List<MenuResponse> responses = menus.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(responses, filter.getPage(), filter.getSize(), total);
    }

    @Override
    public MenuResponse findByIdDto(Integer id) {
        SysMenu menu = menuMapper.findById(id);
        if (menu == null) {
            throw new ResourceNotFoundException("Menu", id);
        }
        return convertToResponse(menu);
    }

    @Override
    @Transactional
    public Integer createMenu(MenuCreateRequest request) {
        log.info("[MenuService] 创建菜单, menuName: {}, menuCode: {}, parentId: {}, menuType: {}",
            request.getMenuName(), request.getMenuCode(), request.getParentId(), request.getMenuType());

        // 构建菜单对象，并清理字符串字段
        SysMenu menu = new SysMenu();
        menu.setParentId(request.getParentId());

        // Trim字符串字段
        if (request.getMenuName() != null) {
            menu.setMenuName(request.getMenuName().trim());
        }
        if (request.getMenuCode() != null) {
            menu.setMenuCode(request.getMenuCode().trim());
        }
        if (request.getPath() != null) {
            String trimmedPath = request.getPath().trim();
            menu.setPath(trimmedPath.isEmpty() ? null : trimmedPath);
        }
        if (request.getComponent() != null) {
            String trimmedComponent = request.getComponent().trim();
            menu.setComponent(trimmedComponent.isEmpty() ? null : trimmedComponent);
        }
        if (request.getPerms() != null) {
            String trimmedPerms = request.getPerms().trim();
            menu.setPerms(trimmedPerms.isEmpty() ? null : trimmedPerms);
        }
        if (request.getIcon() != null) {
            String trimmedIcon = request.getIcon().trim();
            menu.setIcon(trimmedIcon.isEmpty() ? null : trimmedIcon);
        }
        if (request.getRemark() != null) {
            String trimmedRemark = request.getRemark().trim();
            menu.setRemark(trimmedRemark.isEmpty() ? null : trimmedRemark);
        }

        menu.setMenuType(request.getMenuType() != null ? request.getMenuType().getCode() : null);
        menu.setSortOrder(request.getSortOrder());
        menu.setVisible(request.getVisible() != null && request.getVisible() ? 1 : 0);
        menu.setStatus(request.getStatus());

        menuMapper.insert(menu);
        log.info("[MenuService] 菜单创建成功, id: {}", menu.getId());
        return menu.getId();
    }

    @Override
    @Transactional
    public void updateMenu(Integer id, MenuUpdateRequest request) {
        log.info("[MenuService] 更新菜单, id: {}, menuName: {}", id, request.getMenuName());

        SysMenu menu = menuMapper.findById(id);
        if (menu == null) {
            log.warn("[MenuService] 菜单不存在, 无法更新, id: {}", id);
            throw new ResourceNotFoundException("Menu", id);
        }

        // 清理和构建更新对象
        SysMenu updateMenu = new SysMenu();
        updateMenu.setId(id);

        // Trim字符串字段，避免空白字符问题
        if (request.getMenuName() != null) {
            String trimmedName = request.getMenuName().trim();
            updateMenu.setMenuName(trimmedName.isEmpty() ? null : trimmedName);
        }
        if (request.getPath() != null) {
            String trimmedPath = request.getPath().trim();
            updateMenu.setPath(trimmedPath.isEmpty() ? null : trimmedPath);
        }
        if (request.getComponent() != null) {
            String trimmedComponent = request.getComponent().trim();
            updateMenu.setComponent(trimmedComponent.isEmpty() ? null : trimmedComponent);
        }
        if (request.getPerms() != null) {
            String trimmedPerms = request.getPerms().trim();
            updateMenu.setPerms(trimmedPerms.isEmpty() ? null : trimmedPerms);
        }
        if (request.getIcon() != null) {
            String trimmedIcon = request.getIcon().trim();
            updateMenu.setIcon(trimmedIcon.isEmpty() ? null : trimmedIcon);
        }
        if (request.getRemark() != null) {
            String trimmedRemark = request.getRemark().trim();
            updateMenu.setRemark(trimmedRemark.isEmpty() ? null : trimmedRemark);
        }

        updateMenu.setMenuType(request.getMenuType() != null ? request.getMenuType().getCode() : null);
        updateMenu.setSortOrder(request.getSortOrder());
        updateMenu.setVisible(request.getVisible() != null && request.getVisible() ? 1 : 0);
        updateMenu.setStatus(request.getStatus());

        menuMapper.update(updateMenu);
        log.info("[MenuService] 菜单更新成功, id: {}", id);
    }

    @Override
    @Transactional
    public void deleteMenu(Integer id) {
        log.info("[MenuService] 删除菜单, id: {}", id);
        deleteById(id);
        log.info("[MenuService] 菜单删除成功, id: {}", id);
    }

    @Override
    public List<MenuTreeNode> getMenuTree() {
        log.info("[MenuService] 查询完整菜单树");
        List<SysMenu> allMenus = menuMapper.findAll();
        log.info("[MenuService] 查询到 {} 条菜单记录", allMenus.size());

        List<MenuTreeNode> tree = buildMenuTree(allMenus, null);
        log.info("[MenuService] 菜单树构建完成, 根节点数量: {}", tree.size());
        return tree;
    }

    @Override
    public List<MenuTreeNode> getMenuTreeByUserId(Long userId) {
        List<SysMenu> menus = menuMapper.findMenusByUserId(userId);
        log.info("[MenuService] 查询用户菜单, userId={}, 查询到菜单数量={}", userId, menus.size());

        if (!menus.isEmpty()) {
            log.info("[MenuService] 菜单详情:");
            for (SysMenu menu : menus) {
                log.info("  - ID={}, Name={}, ParentId={}, Type={}, Status={}, Visible={}, Path={}",
                    menu.getId(), menu.getMenuName(), menu.getParentId(),
                    menu.getMenuType(), menu.getStatus(), menu.getVisible(), menu.getPath());
            }
        }

        List<MenuTreeNode> tree = buildMenuTree(menus, null);
        log.info("[MenuService] 构建菜单树完成, 根节点数量={}", tree.size());

        return tree;
    }

    @Override
    public List<RoleResponse> findRolesByMenuId(Integer menuId) {
        // Stub implementation - returns empty list
        return Collections.emptyList();
    }

    private MenuResponse convertToResponse(SysMenu menu) {
        MenuResponse response = new MenuResponse();
        response.setId(menu.getId() != null ? menu.getId().longValue() : null);
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
        response.setVisible(menu.getVisible() != null && menu.getVisible() == 1);
        response.setStatus(menu.getStatus());
        response.setCreateBy(menu.getCreateBy());
        if (menu.getCreateTime() != null) {
            response.setCreateTime(menu.getCreateTime().toInstant()
                    .atOffset(java.time.ZoneOffset.UTC));
        }
        response.setRemark(menu.getRemark());
        return response;
    }

    private List<MenuTreeNode> buildMenuTree(List<SysMenu> menus, Integer parentId) {
        List<MenuTreeNode> tree = new ArrayList<>();

        for (SysMenu menu : menus) {
            // 根节点：parentId为null时，匹配parent_id=0或null的菜单
            // 子节点：parentId不为null时，严格匹配parent_id
            boolean isMatch = false;
            if (parentId == null) {
                isMatch = (menu.getParentId() == null || menu.getParentId() == 0);
            } else {
                isMatch = parentId.equals(menu.getParentId());
            }

            if (isMatch) {
                MenuTreeNode node = new MenuTreeNode();
                node.setId(menu.getId() != null ? menu.getId().longValue() : null);
                node.setParentId(menu.getParentId());
                node.setMenuName(menu.getMenuName());
                node.setMenuCode(menu.getMenuCode());
                node.setMenuType(menu.getMenuType() != null
                        ? org.example.jwtjavaeight.enums.MenuTypeEnum.fromCode(menu.getMenuType())
                        : null);
                node.setPath(menu.getPath());
                node.setComponent(menu.getComponent());
                node.setPerms(menu.getPerms());
                node.setIcon(menu.getIcon());
                node.setSortOrder(menu.getSortOrder());
                node.setVisible(menu.getVisible() != null && menu.getVisible() == 1);
                node.setStatus(menu.getStatus());
                node.setCreateBy(menu.getCreateBy());
                if (menu.getCreateTime() != null) {
                    node.setCreateTime(menu.getCreateTime().toInstant()
                            .atOffset(java.time.ZoneOffset.UTC));
                }
                node.setRemark(menu.getRemark());

                List<MenuTreeNode> children = buildMenuTree(menus, menu.getId());
                node.setChildren(children.isEmpty() ? null : children);

                tree.add(node);
            }
        }

        return tree;
    }
}
