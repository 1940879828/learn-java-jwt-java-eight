package org.example.jwtjavaeight.security;

import org.example.jwtjavaeight.domain.entity.SysUser;
import org.example.jwtjavaeight.exception.UserDisabledException;
import org.example.jwtjavaeight.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

  private final UserMapper userMapper;

  public UserDetailsServiceImpl(UserMapper userMapper) {
    this.userMapper = userMapper;
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

    log.debug("[UserDetails] 用户加载成功: {}", sysUser.getUsername());
    return new JwtUserDetails(sysUser);
  }
}
