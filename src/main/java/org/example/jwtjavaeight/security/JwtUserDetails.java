package org.example.jwtjavaeight.security;

import java.util.Collection;
import java.util.Collections;
import org.example.jwtjavaeight.domain.entity.SysUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class JwtUserDetails implements UserDetails {

  private static final long serialVersionUID = 1L;

  private final Long userId;
  private final String username;
  private final String password;
  private final boolean enabled;
  private final boolean accountNonLocked;

  public JwtUserDetails(SysUser sysUser) {
    this.userId = sysUser.getId();
    this.username = sysUser.getUsername();
    this.password = sysUser.getPassword();
    this.enabled = sysUser.getStatus() != null && sysUser.getStatus() == 1;
    // 检查账户是否锁定：如果有锁定时间且未过期，则账户被锁定
    Long lockRemaining = sysUser.getLockRemainingSeconds();
    this.accountNonLocked = !(lockRemaining != null && lockRemaining > 0);
  }

  public Long getUserId() {
    return userId;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return accountNonLocked;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }
}
