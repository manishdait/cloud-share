package com.example.cloud_share_api.token;

import static com.example.cloud_share_api.TestUtils.LOUIS_USERNAME;
import static com.example.cloud_share_api.TestUtils.PETER_USERNAME;
import static com.example.cloud_share_api.TestUtils.TEST_TOKEN;
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

  private User user;

  @BeforeEach
  void setup() {
    user = userRepository.save(createTestUser(PETER_USERNAME));

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
  void canEstablishConncection() {
    Assertions.assertThat(psqlContainer.isCreated());
    Assertions.assertThat(psqlContainer.isRunning());
  }

  @Test
  void prePersist_shouldGenerate_andSaveToken_ifTokenIsNull() {
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
  void prePersist_shouldNotGenerateToken_ifTokenIsNotNull() {
    final String _token = "000000";
    final Token token = Token.builder()
      .token(_token)
      .user(user)
      .type(TokenType.EMAIL_VERIFICATION)
      .expiration(Instant.now().plusSeconds(3600))
      .build();
    final Token result = tokenRepository.save(token);
    
    Assertions.assertThat(result.getToken()).isNotNull();
    Assertions.assertThat(result.getToken()).isEqualTo(_token);
  }

  @Test
  void shouldReturn_tokenOptional_ifTokenExists_withTokenAndType() {
    final String token = TEST_TOKEN;
    final TokenType type = TokenType.EMAIL_VERIFICATION;
    final Optional<Token> result = tokenRepository.findByTokenAndType(token, type);

    Assertions.assertThat(result).isPresent();
  }

  @Test
  void shouldReturn_emptyOptional_ifTokenNotExists_withTokenOrType() {
    final String token1 = TEST_TOKEN;
    final String token2 = "000000";

    final TokenType type1 = TokenType.EMAIL_VERIFICATION;
    final TokenType type2 = TokenType.RESET_PASSWORD;

    // TokenType not exists
    final Optional<Token> result1 = tokenRepository.findByTokenAndType(token1, type2);
    Assertions.assertThat(result1).isEmpty();

    // Token not exists
    final Optional<Token> result2 = tokenRepository.findByTokenAndType(token2, type1);
    Assertions.assertThat(result2).isEmpty();

    // Both not exists
    final Optional<Token> result3 = tokenRepository.findByTokenAndType(token2, type2);
    Assertions.assertThat(result3).isEmpty();
  }

  @Test
  void shouldReturn_tokenList_ifTokensExists_forUserAndType() {
    final User _user = user;
    final TokenType type = TokenType.EMAIL_VERIFICATION;
    final List<Token> result = tokenRepository.findByUserAndType(_user, type);

    Assertions.assertThat(result).isNotEmpty();
    Assertions.assertThat(result).hasSize(1);
  }

  @Test
  void shouldReturn_emptyTokenList_ifTokensNotExists_forUserOrType() {
    final User user1 = user;
    final User user2 = createTestUser(LOUIS_USERNAME);
    user2.setId(200L);

    final TokenType type1 = TokenType.EMAIL_VERIFICATION;
    final TokenType type2 = TokenType.RESET_PASSWORD;

    // TokenType not exists
    final List<Token> result1 = tokenRepository.findByUserAndType(user1, type2);
    Assertions.assertThat(result1).isEmpty();

    // User not exists
    final List<Token> result2 = tokenRepository.findByUserAndType(user2, type1);
    Assertions.assertThat(result2).isEmpty();

    // Both not exists
    final List<Token> result3 = tokenRepository.findByUserAndType(user2, type2);
    Assertions.assertThat(result3).isEmpty();
  }
}
