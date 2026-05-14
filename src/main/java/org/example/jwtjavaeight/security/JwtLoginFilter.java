package org.example.jwtjavaeight.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.example.jwtjavaeight.domain.dto.LoginRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

public class JwtLoginFilter extends UsernamePasswordAuthenticationFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtLoginFilter.class);
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

      // 记录登录尝试（可用于后续的"记住我"功能）
      Boolean remember = loginReq.getRemember();
      if (remember != null && remember) {
        request.setAttribute("remember_me", true);
        log.debug("[LoginFilter] 用户请求记住登录状态");
      }

      String username = loginReq.getUsername() == null ? "" : loginReq.getUsername().trim();
      String password = loginReq.getPassword() == null ? "" : loginReq.getPassword();

      JwtAuthToken authRequest = new JwtAuthToken(username, password);
      setDetails(request, authRequest);
      request.setAttribute("login_username", username);
      return this.getAuthenticationManager().authenticate(authRequest);

    } catch (UnrecognizedPropertyException e) {
      // 未知字段错误
      String fieldName = e.getPropertyName();
      log.warn("[LoginFilter] 登录请求包含未知字段: {}", fieldName);
      throw new BadCredentialsException("请求参数错误：字段 '" + fieldName + "' 不被识别");

    } catch (InvalidFormatException e) {
      // 字段格式错误
      String fieldName = e.getPath().get(0).getFieldName();
      log.warn("[LoginFilter] 登录请求字段格式错误: {}", fieldName);
      throw new BadCredentialsException("请求参数错误：字段 '" + fieldName + "' 格式不正确");

    } catch (MismatchedInputException e) {
      // 必填字段缺失
      log.warn("[LoginFilter] 登录请求缺少必填字段");
      throw new BadCredentialsException("请求参数错误：缺少必填字段");

    } catch (IOException e) {
      // 其他IO错误
      log.error("[LoginFilter] 登录请求解析失败: {}", e.getMessage());
      throw new BadCredentialsException("登录请求格式错误，请检查请求数据");
    }
  }
}
