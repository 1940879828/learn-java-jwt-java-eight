package org.example.jwtjavaeight.exception;

import org.example.jwtjavaeight.enums.ErrorCode;

/** 用户已禁用异常 */
public class UserDisabledException extends BusinessException {

  public UserDisabledException() {
    super(ErrorCode.FORBIDDEN, "用户已被禁用");
  }

}
