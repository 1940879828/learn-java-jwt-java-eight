package org.example.jwtjavaeight.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.jwtjavaeight.domain.entity.SysRoleMenu;

import java.util.List;

@Mapper
public interface RoleMenuMapper {
    /**
     * 批量插入角色菜单关联
     */
    int batchInsert(@Param("list") List<SysRoleMenu> list);

    /**
     * 删除角色的所有菜单
     */
    int deleteByRoleId(@Param("roleId") Integer roleId);

    /**
     * 查询角色的菜单关联
     */
    List<SysRoleMenu> findByRoleId(@Param("roleId") Integer roleId);

    /**
     * 删除角色的指定菜单
     */
    int deleteByRoleIdAndMenuId(@Param("roleId") Integer roleId, @Param("menuId") Integer menuId);
}
