package org.example.jwtjavaeight.utils;

import cn.hutool.crypto.SecureUtil;

public final class HashUtil {

  private HashUtil() {}

  public static String sha256(String input) {
    return SecureUtil.sha256(input);
  }
}
