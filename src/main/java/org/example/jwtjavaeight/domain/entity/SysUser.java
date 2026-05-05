package org.example.jwtjavaeight.domain.entity;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = "password")
public class SysUser {
  /** 用户ID */
  private Long id;
  /** 用户名 */
  private String username;
  /** 密码 */
  private String password;
  /** 状态（1-启用 0-禁用） */
  private Integer status;
  /** 创建时间 */
  private Date createTime;
  /** 登录失败次数 */
  private Integer failedAttempts;
  /** 锁定时间 */
  private Date lockTime;
  /** 锁定剩余秒数（从SQL计算得出，非数据库字段） */
  private Long lockRemainingSeconds;
}
