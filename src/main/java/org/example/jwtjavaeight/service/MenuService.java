package org.example.jwtjavaeight.service;

import org.example.jwtjavaeight.domain.entity.SysMenu;

import java.util.List;

public interface MenuService {
    /**
     * 查询所有菜单
     */
    List<SysMenu> findAll();

    /**
     * 根据ID查询菜单
     */
    SysMenu findById(Integer id);

    /**
     * 根据角色ID查询菜单列表
     */
    List<SysMenu> findMenusByRoleId(Integer roleId);

    /**
     * 根据用户ID查询菜单列表
     */
    List<SysMenu> findMenusByUserId(Long userId);

    /**
     * 创建菜单
     */
    SysMenu create(SysMenu menu);

    /**
     * 更新菜单
     */
    SysMenu update(SysMenu menu);

    /**
     * 删除菜单
     */
    void deleteById(Integer id);
}
