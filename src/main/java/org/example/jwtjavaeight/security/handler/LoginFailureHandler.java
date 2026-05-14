package org.example.jwtjavaeight.security.handler;

import cn.hutool.json.JSONUtil;
import java.io.IOException;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.example.jwtjavaeight.common.Result;
import org.example.jwtjavaeight.enums.ErrorCode;
import org.example.jwtjavaeight.constants.SecurityConstants;
import org.example.jwtjavaeight.domain.dto.LoginFailureData;
import org.example.jwtjavaeight.domain.entity.SysLoginLog;
import org.example.jwtjavaeight.domain.entity.SysUser;
import org.example.jwtjavaeight.mapper.LoginLogMapper;
import org.example.jwtjavaeight.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

  private static final Logger log = LoggerFactory.getLogger(LoginFailureHandler.class);

  private final UserMapper userMapper;
  private final LoginLogMapper loginLogMapper;

  public LoginFailureHandler(UserMapper userMapper, LoginLogMapper loginLogMapper) {
    this.userMapper = userMapper;
    this.loginLogMapper = loginLogMapper;
  }

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    String username = (String) request.getAttribute("login_username");
    String clientIp = getClientIp(request);

    Integer remainingAttempts = null;
    Long lockRemainingSeconds = null;

    if (username != null && !username.isEmpty()) {
      try {
        // 尝试增加失败次数
        // 如果账户已锁定（锁定未过期），更新不会执行
        userMapper.incrementFailedAttempts(
            username,
            SecurityConstants.MAX_LOGIN_ATTEMPTS,
            SecurityConstants.LOGIN_LOCK_DURATION_MS / 1000);

        // 查询用户信息，并从数据库计算剩余锁定时间
        SysUser user = userMapper.findByUsernameWithLockInfo(username);
        if (user != null) {
          if (user.getLockTime() != null) {
            // 使用数据库计算的剩余时间
            Long dbLockRemaining = user.getLockRemainingSeconds();
            if (dbLockRemaining != null && dbLockRemaining > 0) {
              // 账户仍处于锁定状态
              lockRemainingSeconds = dbLockRemaining;
              remainingAttempts = 0;
            } else {
              // 锁定已过期，解锁用户
              userMapper.unlockUser(user.getId());
              // 重新计算剩余尝试次数（解锁后应该重置为最大值）
              remainingAttempts = SecurityConstants.MAX_LOGIN_ATTEMPTS;
            }
          } else {
            // 账户未锁定，计算剩余尝试次数
            int failed = user.getFailedAttempts() != null ? user.getFailedAttempts() : 0;
            remainingAttempts = Math.max(0, SecurityConstants.MAX_LOGIN_ATTEMPTS - failed);
          }
        }
      } catch (Exception e) {
        log.error("[LoginFailure] 更新失败次数异常, username: {}", username, e);
      }
    }

    SysLoginLog loginLog = new SysLoginLog();
    loginLog.setUsername(username != null ? username : "");
    loginLog.setLoginIp(clientIp);
    loginLog.setStatus(0);
    loginLog.setFailReason(exception.getMessage());
    loginLog.setLoginTime(new Date());
    try {
      loginLogMapper.insert(loginLog);
    } catch (Exception e) {
      log.error("[LoginFailure] 写入审计日志异常", e);
    }

    log.info(
        "[LoginFailure] 登录失败, username: {}, ip: {}, reason: {}, 剩余尝试: {}, 锁定剩余: {}s",
        username,
        clientIp,
        exception.getMessage(),
        remainingAttempts,
        lockRemainingSeconds);

    LoginFailureData data = new LoginFailureData(remainingAttempts, lockRemainingSeconds);

    response.setContentType("application/json;charset=UTF-8");
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response
        .getWriter()
        .write(
            JSONUtil.toJsonStr(
                Result.failure(
                    ErrorCode.UNAUTHORIZED.getCode(), exception.getMessage(), data)));
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
