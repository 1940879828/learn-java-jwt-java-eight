package org.example.jwtjavaeight.service.impl;

import io.jsonwebtoken.Claims;
import java.util.Date;
import org.example.jwtjavaeight.config.JwtConfig;
import org.example.jwtjavaeight.constants.SecurityConstants;
import org.example.jwtjavaeight.domain.dto.LoginResponse;
import org.example.jwtjavaeight.domain.dto.RefreshRequest;
import org.example.jwtjavaeight.domain.dto.RegisterRequest;
import org.example.jwtjavaeight.domain.entity.SysRefreshToken;
import org.example.jwtjavaeight.domain.entity.SysUser;
import org.example.jwtjavaeight.exception.TokenExpiredException;
import org.example.jwtjavaeight.exception.UsernameExistsException;
import org.example.jwtjavaeight.mapper.RefreshTokenMapper;
import org.example.jwtjavaeight.mapper.UserMapper;
import org.example.jwtjavaeight.service.AuthService;
import org.example.jwtjavaeight.utils.HashUtil;
import org.example.jwtjavaeight.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

  private final JwtUtil jwtUtil;
  private final JwtConfig jwtConfig;
  private final RefreshTokenMapper refreshTokenMapper;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  public AuthServiceImpl(
      JwtUtil jwtUtil,
      JwtConfig jwtConfig,
      RefreshTokenMapper refreshTokenMapper,
      UserMapper userMapper,
      PasswordEncoder passwordEncoder) {
    this.jwtUtil = jwtUtil;
    this.jwtConfig = jwtConfig;
    this.refreshTokenMapper = refreshTokenMapper;
    this.userMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void register(RegisterRequest registerRequest) {
    log.info("[AuthService] 开始注册, 用户名: {}", registerRequest.getUsername());

    SysUser existingUser = userMapper.findByUsername(registerRequest.getUsername());
    if (existingUser != null) {
      log.warn("[AuthService] 注册失败, 用户名已存在: {}", registerRequest.getUsername());
      throw new UsernameExistsException();
    }

    SysUser user = new SysUser();
    user.setUsername(registerRequest.getUsername());
    user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
    user.setStatus(1);
    user.setCreateTime(new Date());

    userMapper.insert(user);
    log.info("[AuthService] 注册成功, 用户ID: {}, 用户名: {}", user.getId(), user.getUsername());
  }

  @Override
  @Transactional
  public LoginResponse refresh(RefreshRequest refreshRequest) {
    String refreshTokenValue = refreshRequest.getRefreshToken();

    if (!jwtUtil.validateToken(refreshTokenValue)) {
      throw new TokenExpiredException("Refresh Token无效或已过期");
    }

    Claims claims = jwtUtil.parseToken(refreshTokenValue);
    Long userId = claims.get(SecurityConstants.CLAIM_USER_ID, Long.class);
    String jti = claims.getId();

    String tokenHash = HashUtil.sha256(refreshTokenValue);
    SysRefreshToken storedToken = refreshTokenMapper.findByTokenHash(tokenHash);

    if (storedToken == null) {
      throw new TokenExpiredException("Refresh Token不存在");
    }

    if (storedToken.getExpireTime().before(new Date())) {
      refreshTokenMapper.deleteByJtiId(jti);
      throw new TokenExpiredException("Refresh Token已过期");
    }

    refreshTokenMapper.deleteByJtiId(jti);

    SysUser user = userMapper.findById(userId);
    if (user == null) {
      throw new TokenExpiredException("用户不存在");
    }
    if (user.getStatus() == null || user.getStatus() != 1) {
      throw new TokenExpiredException("用户已禁用");
    }

    String authorities = "ROLE_USER";
    String newAccessToken = jwtUtil.generateAccessToken(userId, user.getUsername(), authorities);
    String newRefreshToken = jwtUtil.generateRefreshToken(userId);

    String newTokenHash = HashUtil.sha256(newRefreshToken);
    String newJti = jwtUtil.getJtiFromToken(newRefreshToken);
    Date newExpireTime = jwtUtil.parseToken(newRefreshToken).getExpiration();
    SysRefreshToken newSysRefreshToken = new SysRefreshToken();
    newSysRefreshToken.setUserId(userId);
    newSysRefreshToken.setTokenHash(newTokenHash);
    newSysRefreshToken.setExpireTime(newExpireTime);
    newSysRefreshToken.setJtiId(newJti);
    refreshTokenMapper.insert(newSysRefreshToken);

    return new LoginResponse(newAccessToken, newRefreshToken, jwtConfig.getTokenPrefix().trim());
  }

  @Override
  @Transactional
  public void logout(Long userId) {
    refreshTokenMapper.deleteByUserId(userId);
    log.info("[AuthService] 用户登出成功, userId: {}", userId);
  }

  @Override
  @Transactional
  public void unlockUser(Long userId) {
    userMapper.unlockUser(userId);
    log.info("[AuthService] 用户已解锁, userId: {}", userId);
  }
}
