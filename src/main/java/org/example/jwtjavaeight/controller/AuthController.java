package org.example.jwtjavaeight.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.example.jwtjavaeight.common.Result;
import org.example.jwtjavaeight.domain.dto.LoginResponse;
import org.example.jwtjavaeight.domain.dto.RefreshRequest;
import org.example.jwtjavaeight.security.JwtUserDetails;
import org.example.jwtjavaeight.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证管理", description = "登录、刷新Token、登出接口")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/refresh")
  @Operation(
      summary = "刷新Token",
      description = "使用 Refresh Token 换取新的 Access Token 和 Refresh Token")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "刷新成功"),
      @ApiResponse(responseCode = "401", description = "Refresh Token无效或已过期", content = @Content(schema = @Schema(implementation = Result.class)))
  })
  public Result<LoginResponse> refresh(@Valid @RequestBody RefreshRequest refreshRequest) {
    LoginResponse response = authService.refresh(refreshRequest);
    return Result.success(response);
  }

  @PostMapping("/logout")
  @Operation(
      summary = "用户登出",
      description = "清除当前用户的 Refresh Token 记录",
      security = @SecurityRequirement(name = "Bearer Token"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "登出成功"),
      @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content(schema = @Schema(implementation = Result.class)))
  })
  public Result<Void> logout(@AuthenticationPrincipal JwtUserDetails userDetails) {
    authService.logout(userDetails.getUserId());
    return Result.success();
  }
}
