package org.example.jwtjavaeight.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "用户注册请求参数")
public class RegisterRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 用户名 */
  @NotBlank(message = "用户名不能为空")
  @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
  @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
  @Schema(description = "用户名", example = "testuser")
  private String username;

  /** 密码 */
  @NotBlank(message = "密码不能为空")
  @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
  @Schema(description = "密码", example = "123456")
  private String password;

  /** 邮箱 */
  @Email(message = "邮箱格式不正确")
  @NotBlank(message = "邮箱不能为空")
  @Schema(description = "邮箱", example = "user@example.com")
  private String email;
}
