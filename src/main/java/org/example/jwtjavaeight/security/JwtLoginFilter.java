package org.example.jwtjavaeight.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.example.jwtjavaeight.domain.dto.LoginRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

public class JwtLoginFilter extends UsernamePasswordAuthenticationFilter {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public JwtLoginFilter(
      AuthenticationManager authenticationManager,
      AuthenticationSuccessHandler successHandler,
      AuthenticationFailureHandler failureHandler) {
    setAuthenticationManager(authenticationManager);
    setAuthenticationSuccessHandler(successHandler);
    setAuthenticationFailureHandler(failureHandler);
    setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/api/v1/auth/login", "POST"));
  }

  @Override
  public Authentication attemptAuthentication(
      HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
    try {
      LoginRequest loginReq = objectMapper.readValue(request.getInputStream(), LoginRequest.class);
      String username = loginReq.getUsername() == null ? "" : loginReq.getUsername().trim();
      String password = loginReq.getPassword() == null ? "" : loginReq.getPassword();
      JwtAuthToken authRequest = new JwtAuthToken(username, password);
      setDetails(request, authRequest);
      request.setAttribute("login_username", username);
      return this.getAuthenticationManager().authenticate(authRequest);
    } catch (IOException e) {
      throw new RuntimeException("登录请求解析失败", e);
    }
  }
}
