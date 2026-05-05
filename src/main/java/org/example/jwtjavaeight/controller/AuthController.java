package org.example.jwtjavaeight.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.example.jwtjavaeight.common.Result;
import org.example.jwtjavaeight.domain.dto.LoginRequest;
import org.example.jwtjavaeight.domain.dto.LoginResponse;
import org.example.jwtjavaeight.domain.dto.RefreshRequest;
import org.example.jwtjavaeight.domain.dto.RegisterRequest;
import org.example.jwtjavaeight.security.JwtUserDetails;
import org.example.jwtjavaeight.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "认证管理", description = "登录、刷新Token、登出接口")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  @Operation(
      summary = "用户登录",
      description =
          "用户名+密码登录，返回 Access Token 和 Refresh Token。注意：实际请求由 JwtLoginFilter 在 Filter 链中拦截处理，此接口仅用于 Swagger 文档展示")
  public Result<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
    throw new RuntimeException("此接口由 JwtLoginFilter 拦截处理，不应到达 Controller");
  }

  @PostMapping("/register")
  @Operation(summary = "用户注册", description = "注册新用户账号")
  public Result<Void> register(@Valid @RequestBody RegisterRequest registerRequest) {
    authService.register(registerRequest);
    return Result.success();
  }

  @PostMapping("/refresh")
  @Operation(
      summary = "刷新Token",
      description = "使用 Refresh Token 换取新的 Access Token 和 Refresh Token")
  public Result<LoginResponse> refresh(@Valid @RequestBody RefreshRequest refreshRequest) {
    LoginResponse response = authService.refresh(refreshRequest);
    return Result.success(response);
  }

  @PostMapping("/logout")
  @Operation(
      summary = "用户登出",
      description = "清除当前用户的 Refresh Token 记录",
      security = @SecurityRequirement(name = "Bearer Token"))
  public Result<Void> logout(@AuthenticationPrincipal JwtUserDetails userDetails) {
    authService.logout(userDetails.getUserId());
    return Result.success();
  }

  @PostMapping("/unlock/{userId}")
  @Operation(summary = "解锁用户账户", description = "管理员解锁被锁定的用户账户，重置登录失败次数")
  public Result<Void> unlockUser(@PathVariable Long userId) {
    authService.unlockUser(userId);
    return Result.success();
  }
}
