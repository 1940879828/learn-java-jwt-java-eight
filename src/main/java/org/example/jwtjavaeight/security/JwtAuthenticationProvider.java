package org.example.jwtjavaeight.security;

import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

public class JwtAuthenticationProvider extends DaoAuthenticationProvider {

  public JwtAuthenticationProvider(
      UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    setHideUserNotFoundExceptions(false);
    setUserDetailsService(userDetailsService);
    setPasswordEncoder(passwordEncoder);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return JwtAuthToken.class.isAssignableFrom(authentication);
  }
}
