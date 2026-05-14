package org.example.jwtjavaeight.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.jwtjavaeight.domain.entity.SysUserRole;

import java.util.List;

@Mapper
public interface UserRoleMapper {
    /**
     * 批量插入用户角色关联
     */
    void batchInsert(@Param("list") List<SysUserRole> list);

    /**
     * 删除用户的所有角色
     */
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * 查询用户的角色关联
     */
    List<SysUserRole> findByUserId(@Param("userId") Long userId);

    /**
     * 删除用户的指定角色
     */
    int deleteByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
