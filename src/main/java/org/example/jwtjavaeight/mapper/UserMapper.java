package org.example.jwtjavaeight.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.jwtjavaeight.domain.dto.UserQueryFilter;
import org.example.jwtjavaeight.domain.entity.SysUser;

@Mapper
public interface UserMapper {

  SysUser findByUsername(@Param("username") String username);

  SysUser findByUsernameWithLockInfo(@Param("username") String username);

  SysUser findByEmail(@Param("email") String email);

  SysUser findById(@Param("id") Long id);

  void insert(SysUser user);

  void incrementFailedAttempts(
      @Param("username") String username,
      @Param("maxAttempts") int maxAttempts,
      @Param("lockDurationSeconds") long lockDurationSeconds);

  void resetFailedAttempts(@Param("id") Long id);

  void unlockUser(@Param("id") Long id);

  /**
   * 根据用户ID查询权限列表
   */
  List<String> findPermissionsByUserId(@Param("userId") Long userId);

  /**
   * 分页查询用户列表
   */
  List<SysUser> findByFilter(@Param("filter") UserQueryFilter filter);

  /**
   * 统计符合条件的用户总数
   */
  long countByFilter(@Param("filter") UserQueryFilter filter);

  /**
   * 更新用户信息
   */
  void updateById(SysUser user);

  /**
   * 删除用户（软删除，设置status=0）
   */
  void deleteById(@Param("id") Long id);

  /**
   * 锁定用户
   */
  void lockUser(@Param("id") Long id);

  /**
   * 更新用户密码
   */
  void updatePassword(@Param("id") Long id, @Param("password") String password);
}
