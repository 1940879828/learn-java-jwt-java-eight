package org.example.jwtjavaeight.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.jwtjavaeight.domain.dto.RoleQueryFilter;
import org.example.jwtjavaeight.domain.entity.SysRole;

@Mapper
public interface RoleMapper {
    /**
     * 根据ID查询角色
     */
    SysRole findById(@Param("id") Integer id);

    /**
     * 根据角色编码查询角色
     */
    SysRole findByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 查询所有角色
     */
    List<SysRole> findAll();

    /**
     * 根据用户ID查询角色列表
     */
    List<SysRole> findRolesByUserId(@Param("userId") Long userId);

    /**
     * 插入角色
     */
    int insert(SysRole role);

    /**
     * 更新角色
     */
    int update(SysRole role);

    /**
     * 删除角色
     */
    int deleteById(@Param("id") Integer id);

    /**
     * 分页查询角色列表
     */
    List<SysRole> findByFilter(@Param("filter") RoleQueryFilter filter);

    /**
     * 统计符合条件的角色总数
     */
    long countByFilter(@Param("filter") RoleQueryFilter filter);

    /**
     * 统计使用该角色的用户数
     */
    int countUsersByRoleId(@Param("roleId") Integer roleId);

    /**
     * 根据菜单ID查询拥有该菜单的角色列表
     */
    List<SysRole> findRolesByMenuId(@Param("menuId") Integer menuId);
}
