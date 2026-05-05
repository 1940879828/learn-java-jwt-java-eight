package org.example.jwtjavaeight.security.handler;

import cn.hutool.json.JSONUtil;
import java.io.IOException;
import java.util.Date;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.example.jwtjavaeight.common.Result;
import org.example.jwtjavaeight.config.JwtConfig;
import org.example.jwtjavaeight.domain.dto.LoginResponse;
import org.example.jwtjavaeight.domain.entity.SysLoginLog;
import org.example.jwtjavaeight.domain.entity.SysRefreshToken;
import org.example.jwtjavaeight.mapper.LoginLogMapper;
import org.example.jwtjavaeight.mapper.RefreshTokenMapper;
import org.example.jwtjavaeight.mapper.UserMapper;
import org.example.jwtjavaeight.security.JwtUserDetails;
import org.example.jwtjavaeight.utils.HashUtil;
import org.example.jwtjavaeight.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

  private static final Logger log = LoggerFactory.getLogger(LoginSuccessHandler.class);

  private final JwtUtil jwtUtil;
  private final JwtConfig jwtConfig;
  private final RefreshTokenMapper refreshTokenMapper;
  private final UserMapper userMapper;
  private final LoginLogMapper loginLogMapper;

  public LoginSuccessHandler(JwtUtil jwtUtil, JwtConfig jwtConfig, RefreshTokenMapper refreshTokenMapper,
      UserMapper userMapper, LoginLogMapper loginLogMapper) {
    this.jwtUtil = jwtUtil;
    this.jwtConfig = jwtConfig;
    this.refreshTokenMapper = refreshTokenMapper;
    this.userMapper = userMapper;
    this.loginLogMapper = loginLogMapper;
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException {
    JwtUserDetails userDetails = (JwtUserDetails) authentication.getPrincipal();
    String authorities = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority)
        .collect(Collectors.joining(","));

    String accessToken = jwtUtil.generateAccessToken(userDetails.getUserId(), userDetails.getUsername(), authorities);
    String refreshToken = jwtUtil.generateRefreshToken(userDetails.getUserId());

    String tokenHash = HashUtil.sha256(refreshToken);
    String jti = jwtUtil.getJtiFromToken(refreshToken);
    Date expireTime = jwtUtil.parseToken(refreshToken).getExpiration();
    SysRefreshToken sysRefreshToken = new SysRefreshToken();
    sysRefreshToken.setUserId(userDetails.getUserId());
    sysRefreshToken.setTokenHash(tokenHash);
    sysRefreshToken.setExpireTime(expireTime);
    sysRefreshToken.setJtiId(jti);
    refreshTokenMapper.insert(sysRefreshToken);

    LoginResponse loginResponse = new LoginResponse(accessToken, refreshToken, jwtConfig.getTokenPrefix().trim());

    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(JSONUtil.toJsonStr(Result.success(loginResponse)));

    try {
      userMapper.resetFailedAttempts(userDetails.getUserId());
    } catch (Exception e) {
      log.error("[LoginSuccess] 重置失败次数异常, userId: {}", userDetails.getUserId(), e);
    }

    SysLoginLog loginLog = new SysLoginLog();
    loginLog.setUsername(userDetails.getUsername());
    loginLog.setLoginIp(getClientIp(request));
    loginLog.setStatus(1);
    loginLog.setLoginTime(new Date());
    try {
      loginLogMapper.insert(loginLog);
    } catch (Exception e) {
      log.error("[LoginSuccess] 写入审计日志异常", e);
    }

    log.info("[LoginSuccess] 登录成功, username: {}, ip: {}", userDetails.getUsername(), getClientIp(request));
  }

  private String getClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("X-Real-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }
    if (ip != null && ip.contains(",")) {
      ip = ip.split(",")[0].trim();
    }
    return ip;
  }
}
