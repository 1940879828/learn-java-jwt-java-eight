package org.example.jwtjavaeight.security;

import java.util.Collection;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class JwtAuthToken extends UsernamePasswordAuthenticationToken {

  public JwtAuthToken(Object principal, Object credentials) {
    super(principal, credentials);
  }

  public JwtAuthToken(
      Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
    super(principal, credentials, authorities);
  }
}
