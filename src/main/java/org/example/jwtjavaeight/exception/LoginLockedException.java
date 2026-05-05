package org.example.jwtjavaeight.exception;

import org.springframework.security.authentication.LockedException;

public class LoginLockedException extends LockedException {

  public LoginLockedException() {
    super("账户已锁定，请联系管理员解锁");
  }

  public LoginLockedException(String message) {
    super(message);
  }
}
