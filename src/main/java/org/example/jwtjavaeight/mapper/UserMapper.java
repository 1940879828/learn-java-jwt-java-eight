package org.example.jwtjavaeight.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.jwtjavaeight.domain.entity.SysUser;

@Mapper
public interface UserMapper {

  SysUser findByUsername(@Param("username") String username);

  SysUser findByUsernameWithLockInfo(@Param("username") String username);

  SysUser findByEmail(@Param("email") String email);

  SysUser findById(@Param("id") Long id);

  int insert(SysUser user);

  int incrementFailedAttempts(
      @Param("username") String username,
      @Param("maxAttempts") int maxAttempts,
      @Param("lockDurationSeconds") long lockDurationSeconds);

  int resetFailedAttempts(@Param("id") Long id);

  int unlockUser(@Param("id") Long id);

  /**
   * 根据用户ID查询权限列表
   */
  java.util.List<String> findPermissionsByUserId(@Param("userId") Long userId);
}
