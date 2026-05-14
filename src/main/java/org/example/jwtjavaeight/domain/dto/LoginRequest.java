package org.example.jwtjavaeight.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "登录请求参数")
public class LoginRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 用户名 */
  @NotBlank(message = "用户名不能为空")
  @Schema(description = "用户名", example = "admin")
  private String username;

  /** 密码 */
  @NotBlank(message = "密码不能为空")
  @Schema(description = "密码", example = "123456")
  private String password;

  /** 记住我（7天免登录） */
  @Schema(description = "记住我", example = "true")
  private Boolean remember;
}
