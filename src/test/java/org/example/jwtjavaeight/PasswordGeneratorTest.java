package org.example.jwtjavaeight;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGeneratorTest {

  @Test
  public void generatePassword() {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    String rawPassword = "123456";
    String encodedPassword = encoder.encode(rawPassword);

    System.out.println("========================================");
    System.out.println("原始密码: " + rawPassword);
    System.out.println("Bcrypt加密后: " + encodedPassword);
    System.out.println("========================================");

    // 验证密码是否匹配
    boolean matches = encoder.matches(rawPassword, encodedPassword);
    System.out.println("验证结果: " + matches);
  }
}
