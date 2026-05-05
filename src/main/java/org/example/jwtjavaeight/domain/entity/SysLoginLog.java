package org.example.jwtjavaeight.domain.entity;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SysLoginLog {
  /** 日志ID */
  private Long id;
  /** 用户名 */
  private String username;
  /** 登录IP */
  private String loginIp;
  /** 状态（1-成功 0-失败） */
  private Integer status;
  /** 失败原因 */
  private String failReason;
  /** 登录时间 */
  private Date loginTime;
}
