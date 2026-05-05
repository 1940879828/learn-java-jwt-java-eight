package org.example.jwtjavaeight.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.UUID;
import org.example.jwtjavaeight.config.JwtConfig;
import org.example.jwtjavaeight.constants.SecurityConstants;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  private final JwtConfig jwtConfig;

  public JwtUtil(JwtConfig jwtConfig) {
    this.jwtConfig = jwtConfig;
  }

  public String generateAccessToken(Long userId, String username, String authorities) {
    Date now = new Date();
    Date expiration = new Date(now.getTime() + jwtConfig.getAccessTokenExpiration());
    String jti = UUID.randomUUID().toString();

    return Jwts.builder()
        .setId(jti)
        .setSubject(username)
        .claim(SecurityConstants.CLAIM_USER_ID, userId)
        .claim(SecurityConstants.CLAIM_AUTHORITIES, authorities)
        .setIssuedAt(now)
        .setExpiration(expiration)
        .signWith(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes()), SignatureAlgorithm.HS256)
        .compact();
  }

  public String generateRefreshToken(Long userId) {
    Date now = new Date();
    Date expiration = new Date(now.getTime() + jwtConfig.getRefreshTokenExpiration());
    String jti = UUID.randomUUID().toString();

    return Jwts.builder()
        .setId(jti)
        .claim(SecurityConstants.CLAIM_USER_ID, userId)
        .setIssuedAt(now)
        .setExpiration(expiration)
        .signWith(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes()), SignatureAlgorithm.HS256)
        .compact();
  }

  public Claims parseToken(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(jwtConfig.getSecret().getBytes())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  public boolean validateToken(String token) {
    try {
      parseToken(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public String getJtiFromToken(String token) {
    return parseToken(token).getId();
  }

  public Long getUserIdFromToken(String token) {
    return parseToken(token).get(SecurityConstants.CLAIM_USER_ID, Long.class);
  }
}
