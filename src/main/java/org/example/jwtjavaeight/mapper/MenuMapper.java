package org.example.jwtjavaeight.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.jwtjavaeight.domain.entity.SysMenu;

import java.util.List;

@Mapper
public interface MenuMapper {
    /**
     * 根据ID查询菜单
     */
    SysMenu findById(@Param("id") Integer id);

    /**
     * 查询所有菜单
     */
    List<SysMenu> findAll();

    /**
     * 根据角色ID查询菜单列表
     */
    List<SysMenu> findMenusByRoleId(@Param("roleId") Integer roleId);

    /**
     * 根据用户ID查询菜单列表
     */
    List<SysMenu> findMenusByUserId(@Param("userId") Long userId);

    /**
     * 插入菜单
     */
    int insert(SysMenu menu);

    /**
     * 更新菜单
     */
    int update(SysMenu menu);

    /**
     * 删除菜单
     */
    int deleteById(@Param("id") Integer id);
}
