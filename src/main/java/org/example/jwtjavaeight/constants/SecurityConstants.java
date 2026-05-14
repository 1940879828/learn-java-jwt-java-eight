package org.example.jwtjavaeight.constants;

public final class SecurityConstants {

  private SecurityConstants() {}

  public static final String CLAIM_USER_ID = "userId";

  public static final String CLAIM_AUTHORITIES = "authorities";

  public static final int MAX_LOGIN_ATTEMPTS = 5;

  public static final long LOGIN_LOCK_DURATION_MS = 2 * 60 * 60 * 1000;
}
