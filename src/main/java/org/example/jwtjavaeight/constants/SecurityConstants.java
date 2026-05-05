package org.example.jwtjavaeight.constants;

public final class SecurityConstants {

  private SecurityConstants() {}

  public static final String TOKEN_PREFIX = "Bearer ";

  public static final String AUTHORIZATION_HEADER = "Authorization";

  public static final String CLAIM_USER_ID = "userId";

  public static final String CLAIM_USERNAME = "username";

  public static final String CLAIM_AUTHORITIES = "authorities";

  public static final String ROLE_PREFIX = "ROLE_";

  public static final String ANONYMOUS_USER = "anonymousUser";

  public static final String[] PUBLIC_URLS = {"/auth/**"};

  public static final int MAX_LOGIN_ATTEMPTS = 5;

  public static final long LOGIN_LOCK_DURATION_MS = 2 * 60 * 60 * 1000;
}
