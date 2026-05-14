package org.example.jwtjavaeight.exception;

import org.example.jwtjavaeight.enums.ErrorCode;

/** 用户名已存在异常 */
public class UsernameExistsException extends BusinessException {

  public UsernameExistsException() {
    super(ErrorCode.DUPLICATE_RESOURCE, "用户名已存在");
  }

}
