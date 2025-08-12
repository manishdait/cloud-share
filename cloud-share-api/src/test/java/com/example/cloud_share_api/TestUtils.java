package com.example.cloud_share_api;

import java.time.Instant;

import com.example.cloud_share_api.token.Token;
import com.example.cloud_share_api.token.TokenType;
import com.example.cloud_share_api.user.User;

public class TestUtils {
  /*
   * User
   */
  public static final String PETER_USERNAME = "peter@dev.in";
  public static final String LOUIS_USERNAME = "louis@dev.in";
  
  public static User createTestUser(String username) {
    return createTestUser(username, "password");
  }
  public static User createTestUser(String username, String password) {
    return User.builder()
      .firstname("Test Firstname")
      .lastname("Test Lastname")
      .email(username)
      .password(password)
      .build();
  }

  /*
   * Token
   */
  public static String TEST_TOKEN = "T0K3N0";

  public static Token createTestToken(String token, TokenType tokenType) {
    return Token.builder()
      .token(token)
      .type(tokenType)
      .expiration(Instant.now().plusSeconds(3600))
      .build();
  }
}
