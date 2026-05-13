package org.example.jwtjavaeight.security;

import org.example.jwtjavaeight.domain.entity.SysRole;
import org.example.jwtjavaeight.domain.entity.SysUser;
import org.example.jwtjavaeight.exception.UserDisabledException;
import org.example.jwtjavaeight.mapper.RoleMapper;
import org.example.jwtjavaeight.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

  private final UserMapper userMapper;
  private final RoleMapper roleMapper;

  public UserDetailsServiceImpl(UserMapper userMapper, RoleMapper roleMapper) {
    this.userMapper = userMapper;
    this.roleMapper = roleMapper;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    log.debug("[UserDetails] 加载用户: {}", username);
    SysUser sysUser = userMapper.findByUsernameWithLockInfo(username);

    if (sysUser == null) {
      log.warn("[UserDetails] 用户不存在: {}", username);
      throw new UsernameNotFoundException("用户不存在: " + username);
    }

    if (sysUser.getStatus() == null || sysUser.getStatus() != 1) {
      log.warn("[UserDetails] 用户已禁用: {}", username);
      throw new UserDisabledException();
    }

    if (sysUser.getLockTime() != null) {
      Long lockRemaining = sysUser.getLockRemainingSeconds();
      if (lockRemaining != null && lockRemaining > 0) {
        log.warn("[UserDetails] 账户已锁定: {}, 剩余锁定时间: {}秒", username, lockRemaining);
        // 不抛出异常，通过 UserDetails.isAccountNonLocked() 返回锁定状态
        // Spring Security 会自动处理并抛出标准的 LockedException
      } else {
        userMapper.unlockUser(sysUser.getId());
        sysUser.setFailedAttempts(0);
        sysUser.setLockTime(null);
        log.info("[UserDetails] 账户锁定已过期，自动解锁: {}", username);
      }
    }

    // 加载用户的角色
    List<SysRole> roles = roleMapper.findRolesByUserId(sysUser.getId());
    log.debug("[UserDetails] 用户 {} 拥有 {} 个角色", username, roles.size());

    // 加载用户的权限
    List<String> permissions = userMapper.findPermissionsByUserId(sysUser.getId());
    log.debug("[UserDetails] 用户 {} 拥有 {} 个权限", username, permissions.size());

    // 构建 GrantedAuthority 列表
    List<GrantedAuthority> authorities = new ArrayList<>();

    // 添加角色（以ROLE_开头）
    for (SysRole role : roles) {
      authorities.add(new SimpleGrantedAuthority(role.getRoleCode()));
    }

    // 添加权限
    for (String perm : permissions) {
      authorities.add(new SimpleGrantedAuthority(perm));
    }

    log.debug("[UserDetails] 用户加载成功: {}, 权限总数: {}", sysUser.getUsername(), authorities.size());
    return new JwtUserDetails(sysUser, authorities);
  }
}
