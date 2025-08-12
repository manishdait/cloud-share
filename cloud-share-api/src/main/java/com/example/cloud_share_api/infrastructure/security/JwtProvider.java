package com.example.cloud_share_api.infrastructure.security;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtProvider {
  @Value("${spring.security.jwt.secret-key}")
  private String secretKey;

  @Value("${spring.security.jwt.expiration}")
  private Integer expiration;

  public String generateToken(String username, Integer expiration) {
    return Jwts.builder()
      .subject(username)
      .issuedAt(Date.from(Instant.now()))
      .expiration(Date.from(Instant.now().plusSeconds(expiration)))
      .signWith(getKey())
      .compact();
  }

  public String generateToken(String username) {
    return generateToken(username, this.expiration);
  }

  public Claims extractClaims(String token) {
    try {
      return Jwts.parser()
        .verifyWith(getKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
    } catch (JwtException e) {
      throw new JwtException(e.getMessage());
    } catch (Exception e) {
      throw new IllegalArgumentException();
    }
  }

  public String getUsername(String token) {
    return extractClaims(token).getSubject();
  }

  public boolean isExpired(String token) {
    return extractClaims(token).getExpiration().before(new Date());
  }

  public boolean validToken(UserDetails userDetails, String token) {
    return userDetails.getUsername().equals(getUsername(token)) && !isExpired(token);
  }

  private SecretKey getKey() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.secretKey));
  }
}
