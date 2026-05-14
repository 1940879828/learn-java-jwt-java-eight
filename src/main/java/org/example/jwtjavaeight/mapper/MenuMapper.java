package org.example.jwtjavaeight.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.jwtjavaeight.domain.dto.MenuQueryFilter;
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
    void insert(SysMenu menu);

    /**
     * 更新菜单
     */
    void update(SysMenu menu);

    /**
     * 删除菜单
     */
    void deleteById(@Param("id") Integer id);

    /**
     * 根据过滤条件分页查询菜单
     */
    List<SysMenu> findByFilter(@Param("filter") MenuQueryFilter filter);

    /**
     * 根据过滤条件统计菜单数量
     */
    long countByFilter(@Param("filter") MenuQueryFilter filter);
}
