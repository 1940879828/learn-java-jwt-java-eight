package org.example.jwtjavaeight.exception;

import org.example.jwtjavaeight.common.ResultCode;

/** 用户已禁用异常 */
public class UserDisabledException extends BusinessException {

  public UserDisabledException() {
    super(ResultCode.BAD_CREDENTIALS.getCode(), "用户已被禁用");
  }

  public UserDisabledException(String message) {
    super(ResultCode.BAD_CREDENTIALS.getCode(), message);
  }
}
