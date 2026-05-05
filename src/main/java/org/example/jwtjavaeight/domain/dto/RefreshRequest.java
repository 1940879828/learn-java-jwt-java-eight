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
@Schema(description = "刷新Token请求参数")
public class RefreshRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 刷新令牌 */
  @NotBlank(message = "refreshToken不能为空")
  @Schema(description = "刷新令牌")
  private String refreshToken;
}
