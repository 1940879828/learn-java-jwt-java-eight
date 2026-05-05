package org.example.jwtjavaeight.domain.entity;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SysRefreshToken {
  /** 令牌ID */
  private Long id;
  /** 用户ID */
  private Long userId;
  /** 令牌哈希值 */
  private String tokenHash;
  /** 过期时间 */
  private Date expireTime;
  /** 设备信息 */
  private String deviceInfo;
  /** JWT ID */
  private String jtiId;
  /** 创建时间 */
  private Date createTime;
}
