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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private MenuMapper menuMapper;

    @Override
    public List<SysMenu> findAll() {
        return menuMapper.findAll();
    }

    @Override
    public SysMenu findById(Integer id) {
        return menuMapper.findById(id);
    }

    @Override
    public List<SysMenu> findMenusByRoleId(Integer roleId) {
        return menuMapper.findMenusByRoleId(roleId);
    }

    @Override
    public List<SysMenu> findMenusByUserId(Long userId) {
        return menuMapper.findMenusByUserId(userId);
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
        List<SysMenu> menus = menuMapper.findByFilter(filter);
        long total = menuMapper.countByFilter(filter);

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
        SysMenu menu = new SysMenu();
        menu.setParentId(request.getParentId());
        menu.setMenuName(request.getMenuName());
        menu.setMenuCode(request.getMenuCode());
        menu.setMenuType(request.getMenuType() != null ? request.getMenuType().getCode() : null);
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setPerms(request.getPerms());
        menu.setIcon(request.getIcon());
        menu.setSortOrder(request.getSortOrder());
        menu.setVisible(request.getVisible());
        menu.setStatus(request.getStatus());
        menu.setRemark(request.getRemark());

        menuMapper.insert(menu);
        return menu.getId();
    }

    @Override
    @Transactional
    public void updateMenu(Integer id, MenuUpdateRequest request) {
        SysMenu menu = menuMapper.findById(id);
        if (menu == null) {
            throw new ResourceNotFoundException("Menu", id);
        }

        SysMenu updateMenu = new SysMenu();
        updateMenu.setId(id);
        updateMenu.setMenuName(request.getMenuName());
        updateMenu.setMenuType(request.getMenuType() != null ? request.getMenuType().getCode() : null);
        updateMenu.setPath(request.getPath());
        updateMenu.setComponent(request.getComponent());
        updateMenu.setPerms(request.getPerms());
        updateMenu.setIcon(request.getIcon());
        updateMenu.setSortOrder(request.getSortOrder());
        updateMenu.setVisible(request.getVisible());
        updateMenu.setStatus(request.getStatus());
        updateMenu.setRemark(request.getRemark());

        menuMapper.update(updateMenu);
    }

    @Override
    @Transactional
    public void deleteMenu(Integer id) {
        deleteById(id);
    }

    @Override
    public List<MenuTreeNode> getMenuTree() {
        List<SysMenu> allMenus = menuMapper.findAll();
        return buildMenuTree(allMenus, null);
    }

    @Override
    public List<MenuTreeNode> getMenuTreeByUserId(Long userId) {
        List<SysMenu> menus = menuMapper.findMenusByUserId(userId);
        return buildMenuTree(menus, null);
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

    private List<MenuTreeNode> buildMenuTree(List<SysMenu> menus, Integer parentId) {
        List<MenuTreeNode> tree = new ArrayList<>();

        for (SysMenu menu : menus) {
            if ((parentId == null && menu.getParentId() == null) ||
                (parentId != null && parentId.equals(menu.getParentId()))) {
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
                node.setVisible(menu.getVisible());
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
