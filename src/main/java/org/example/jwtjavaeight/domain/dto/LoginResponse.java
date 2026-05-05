package org.example.jwtjavaeight.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "登录/刷新响应结果")
public class LoginResponse implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 访问令牌 */
  @Schema(description = "访问令牌")
  private String accessToken;

  /** 刷新令牌 */
  @Schema(description = "刷新令牌")
  private String refreshToken;

  /** 令牌类型 */
  @Schema(description = "令牌类型", example = "Bearer")
  private String tokenType;

  public LoginResponse(String accessToken, String refreshToken, String tokenType) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.tokenType = tokenType;
  }
}
