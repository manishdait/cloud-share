package com.example.cloud_share_api.token;

import static com.example.cloud_share_api.TestUtils.createTestToken;
import static com.example.cloud_share_api.TestUtils.createTestUser;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.example.cloud_share_api.user.User;
import com.example.cloud_share_api.user.UserRepository;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class TokenRepositoryTest {
  @Container
  @ServiceConnection
  private static PostgreSQLContainer<?> psqlContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:alpine"));

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TokenRepository tokenRepository;

  private static final String TEST_TOKEN = "TOKEN3";
  private User user;

  @BeforeEach
  void setup() {
    user = userRepository.save(createTestUser("user@test.in", "password"));
    
    Token token = createTestToken(TEST_TOKEN, TokenType.EMAIL_VERIFICATION);
    token.setUser(user);
    tokenRepository.save(token);
  }

  @AfterEach
  void purge() {
    tokenRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void shouldConnectToDatabase() {
    Assertions.assertThat(psqlContainer.isCreated());
    Assertions.assertThat(psqlContainer.isRunning());
  }

  @Test
  void shouldGenerateToken_whenTokenIsNull() {
    final Token token = Token.builder()
      .user(user)
      .type(TokenType.EMAIL_VERIFICATION)
      .expiration(Instant.now().plusSeconds(3600))
      .build();

    final Token result = tokenRepository.save(token);
    
    Assertions.assertThat(result.getToken()).isNotNull();
    Assertions.assertThat(result.getToken()).hasSize(6);
  }

  @Test
  void shouldNotGenerateToken_whenTokenIsProvided() {
    final Token token = createTestToken("000000", TokenType.EMAIL_VERIFICATION);
    token.setUser(user);
    
    final Token result = tokenRepository.save(token);
    
    Assertions.assertThat(result.getToken()).isNotNull();
    Assertions.assertThat(result.getToken()).isEqualTo("000000");
  }

  @Test
  void shouldFindToken_whenTokenAndTypeExist() {
    final String token = TEST_TOKEN;
    final TokenType type = TokenType.EMAIL_VERIFICATION;
    final Optional<Token> result = tokenRepository.findByTokenAndType(token, type);

    Assertions.assertThat(result).isPresent();
    Assertions.assertThat(result.get().getToken()).isEqualTo(TEST_TOKEN);
  }

  @Test
  void shouldReturnEmptyOptional_whenTokenDoesNotExist() {
    // Wrong Token
    final Optional<Token> result1 = tokenRepository.findByTokenAndType("invalid-test-token", TokenType.EMAIL_VERIFICATION);
    Assertions.assertThat(result1).isEmpty();

    // Wrong Token Type
    final Optional<Token> result2 = tokenRepository.findByTokenAndType(TEST_TOKEN, TokenType.RESET_PASSWORD);
    Assertions.assertThat(result2).isEmpty();

    // Wrong Token and Token Type
    final Optional<Token> result3 = tokenRepository.findByTokenAndType("invalid-test-token", TokenType.RESET_PASSWORD);
    Assertions.assertThat(result3).isEmpty();
  }

  @Test
  void shouldReturnTokenList_whenTokensExistForUserAndType() {
    final TokenType type = TokenType.EMAIL_VERIFICATION;
    final List<Token> result = tokenRepository.findByUserAndType(user, type);

    Assertions.assertThat(result).isNotEmpty();
    Assertions.assertThat(result).hasSize(1);
  }

  @Test
  void shouldReturnEmptyList_whenNoTokensExistForUserOrType() {
    final User anotherUser = createTestUser("anotheruser@test.in", "password");
    anotherUser.setId(1L);

    // No token with token type exists
    final List<Token> result1 = tokenRepository.findByUserAndType(user, TokenType.RESET_PASSWORD);
    Assertions.assertThat(result1).isEmpty();

    // No token with user exists
    final List<Token> result2 = tokenRepository.findByUserAndType(anotherUser, TokenType.EMAIL_VERIFICATION);
    Assertions.assertThat(result2).isEmpty();

    // No token exists for both user and token type
    final List<Token> result3 = tokenRepository.findByUserAndType(anotherUser, TokenType.RESET_PASSWORD);
    Assertions.assertThat(result3).isEmpty();
  }
}
