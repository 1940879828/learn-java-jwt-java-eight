package org.example.jwtjavaeight.domain.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginFailureData implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 剩余尝试次数 */
  private Integer remainingAttempts;

  /** 锁定时剩余解锁秒数（未锁定时为 null） */
  private Long lockRemainingSeconds;

  public LoginFailureData(Integer remainingAttempts, Long lockRemainingSeconds) {
    this.remainingAttempts = remainingAttempts;
    this.lockRemainingSeconds = lockRemainingSeconds;
  }
}
