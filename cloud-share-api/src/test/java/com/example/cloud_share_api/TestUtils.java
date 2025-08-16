package com.example.cloud_share_api;

import java.time.Instant;

import com.example.cloud_share_api.file.File;
import com.example.cloud_share_api.token.Token;
import com.example.cloud_share_api.token.TokenType;
import com.example.cloud_share_api.user.User;

public class TestUtils {
  //User
  public static User createTestUser(String email, String password) {
    return User.builder()
      .firstname("Test")
      .lastname("User")
      .email(email)
      .password(password)
      .credit(5)
      .build();
  }

  //Token
  public static Token createTestToken(String token, TokenType tokenType) {
    return Token.builder()
      .token(token)
      .type(tokenType)
      .expiration(Instant.now().plusSeconds(3600))
      .build();
  }

  //File
  public static File createFile(String fileUuid) {
    return File.builder()
      .uuid(fileUuid)
      .name("testfile.txt")
      .type("text/plain")
      .size(1234L)
      .isPublic(false)
      .location("uploads/testfile.txt")
      .uploadedAt(Instant.now())
      .build();
  }
}
