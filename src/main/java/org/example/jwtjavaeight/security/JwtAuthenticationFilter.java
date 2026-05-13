package org.example.jwtjavaeight.security;

import cn.hutool.json.JSONUtil;
import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.example.jwtjavaeight.common.Result;
import org.example.jwtjavaeight.enums.ErrorCode;
import org.example.jwtjavaeight.config.JwtConfig;
import org.example.jwtjavaeight.constants.SecurityConstants;
import org.example.jwtjavaeight.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  private final JwtUtil jwtUtil;
  private final JwtConfig jwtConfig;

  public JwtAuthenticationFilter(JwtUtil jwtUtil, JwtConfig jwtConfig) {
    this.jwtUtil = jwtUtil;
    this.jwtConfig = jwtConfig;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(jwtConfig.getHeader());

    if (!StringUtils.hasText(header) || !header.startsWith(jwtConfig.getTokenPrefix())) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = header.substring(jwtConfig.getTokenPrefix().length());

    try {
      if (!jwtUtil.validateToken(token)) {
        log.warn("[JWT Filter] Token验证失败");
        writeUnauthorized(response);
        return;
      }

      Claims claims = jwtUtil.parseToken(token);
      Long userId = claims.get(SecurityConstants.CLAIM_USER_ID, Long.class);
      String username = claims.getSubject();
      String authoritiesStr = claims.get(SecurityConstants.CLAIM_AUTHORITIES, String.class);

      List<SimpleGrantedAuthority> authorities =
          Stream.of(authoritiesStr.split(","))
              .map(SimpleGrantedAuthority::new)
              .collect(Collectors.toList());

      JwtUserDetails userDetails = new JwtUserDetails(buildUserEntity(userId, username));

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
      SecurityContextHolder.getContext().setAuthentication(authentication);

      log.debug("[JWT Filter] 用户 {} 认证成功", username);

    } catch (Exception e) {
      log.error("[JWT Filter] Token解析异常: {}", e.getMessage());
      writeUnauthorized(response);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private void writeUnauthorized(HttpServletResponse response) throws IOException {
    SecurityContextHolder.clearContext();
    response.setContentType("application/json;charset=UTF-8");
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response
        .getWriter()
        .write(
            JSONUtil.toJsonStr(Result.failure(ErrorCode.UNAUTHORIZED.getCode(), "Token已过期或无效")));
  }

  private org.example.jwtjavaeight.domain.entity.SysUser buildUserEntity(
      Long userId, String username) {
    org.example.jwtjavaeight.domain.entity.SysUser user =
        new org.example.jwtjavaeight.domain.entity.SysUser();
    user.setId(userId);
    user.setUsername(username);
    user.setStatus(1);
    return user;
  }
}
