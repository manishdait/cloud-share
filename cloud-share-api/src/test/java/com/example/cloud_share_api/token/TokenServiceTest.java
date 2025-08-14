package com.example.cloud_share_api.token;

import static com.example.cloud_share_api.TestUtils.PETER_USERNAME;
import static com.example.cloud_share_api.TestUtils.TEST_TOKEN;
import static com.example.cloud_share_api.TestUtils.createTestToken;
import static com.example.cloud_share_api.TestUtils.createTestUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.cloud_share_api.infrastructure.exceptions.ExpiredTokenException;
import com.example.cloud_share_api.infrastructure.exceptions.InvalidTokenException;
import com.example.cloud_share_api.infrastructure.exceptions.TokenAlreadyUsedException;
import com.example.cloud_share_api.user.User;

@ExtendWith(MockitoExtension.class)
public class TokenServiceTest {
  @Mock
  private TokenRepository tokenRepository;

  private TokenService tokenService;

  @Captor
  private ArgumentCaptor<Token> tokenCaptor;

  @BeforeEach
  void setup() {
    tokenService = new TokenService(tokenRepository);
  }

  @AfterEach
  void purge() {
    tokenService = null;
  }

  @Test
  void shouldRetrun_tokenString_whenCallGenerateToken() {
    final User user = createTestUser(PETER_USERNAME);
    final TokenType type = TokenType.EMAIL_VERIFICATION;

    when(tokenRepository.save(any(Token.class))).thenAnswer((invocation) -> {
      Token tokenToSave = invocation.getArgument(0);
      tokenToSave.generateToken();
      return tokenToSave;
    });

    final String result = tokenService.generateToken(user, type);
    verify(tokenRepository, times(1)).save(tokenCaptor.capture());

    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result).hasSize(6);

    final Token capture = tokenCaptor.getValue();

    Assertions.assertThat(capture).isNotNull();
    Assertions.assertThat(capture.getType()).isEqualTo(type);
    Assertions.assertThat(capture.getUser()).isEqualTo(user);
    Assertions.assertThat(capture.isUsed()).isFalse();
    Assertions.assertThat(capture.getUsedAt()).isNull();
  }

  @Test
  void shouldNotThrowException_onValidatingToken() {
    final Token mockToken = createTestToken(TEST_TOKEN, TokenType.EMAIL_VERIFICATION);
    
    final User user = createTestUser(PETER_USERNAME);
    mockToken.setUser(user);

    final TokenType type = TokenType.EMAIL_VERIFICATION;
    final String token = TEST_TOKEN;

    when(tokenRepository.findByTokenAndType(eq(token), eq(type)))
      .thenReturn(Optional.of(mockToken));
    
      tokenService.validateToken(token, type, user);
    
    verify(tokenRepository, times(1))
      .findByTokenAndType(eq(token), eq(type));
    verify(tokenRepository, times(1)).save(any(Token.class));

    Assertions.assertThat(mockToken.getUsedAt()).isNotNull();
    Assertions.assertThat(mockToken.isUsed()).isTrue();
  }

  @Test
  void shouldThrowException_onValidatingToken_IfTokenExpired() {
    final Token mockToken = Mockito.mock(Token.class);

    final User user = createTestUser(PETER_USERNAME);
    final TokenType type = TokenType.EMAIL_VERIFICATION;
    final String token = TEST_TOKEN;

    when(tokenRepository.findByTokenAndType(eq(token), eq(type)))
      .thenReturn(Optional.of(mockToken));
    when(mockToken.getUser()).thenReturn(user);
    when(mockToken.isUsed()).thenReturn(false);
    when(mockToken.isExpired()).thenReturn(true);

    Assertions.assertThatThrownBy(() -> tokenService.validateToken(token, type, user))
      .isInstanceOf(ExpiredTokenException.class);
  }

  @Test
  void shouldThrowException_onValidatingToken_IfAlreadyUsed() {
    final Token mockToken = Mockito.mock(Token.class);

    final User user = createTestUser(PETER_USERNAME);
    final TokenType type = TokenType.EMAIL_VERIFICATION;
    final String token = TEST_TOKEN;

    when(tokenRepository.findByTokenAndType(eq(token), eq(type)))
      .thenReturn(Optional.of(mockToken));
    when(mockToken.getUser()).thenReturn(user);
    when(mockToken.isUsed()).thenReturn(true);

    Assertions.assertThatThrownBy(() -> tokenService.validateToken(token, type, user))
      .isInstanceOf(TokenAlreadyUsedException.class);
  }

  @Test
  void shouldThrowException_onValidatingToken_IfHasInvalidUser() {
    final Token mockToken = Mockito.mock(Token.class);
    final User mockUser = Mockito.mock(User.class);

    final User user = createTestUser(PETER_USERNAME);
    final TokenType type = TokenType.EMAIL_VERIFICATION;
    final String token = TEST_TOKEN;

    when(tokenRepository.findByTokenAndType(eq(token), eq(type)))
      .thenReturn(Optional.of(mockToken));
    when(mockToken.getUser()).thenReturn(mockUser);
    when(mockUser.getUsername()).thenReturn("mockUser@test.in");

    Assertions.assertThatThrownBy(() -> tokenService.validateToken(token, type, user))
      .isInstanceOf(InvalidTokenException.class);
  }

  @Test
  void shouldThrowException_onValidatingToken_ForInvalidToken() {
    final User user = createTestUser(PETER_USERNAME);
    final TokenType type = TokenType.EMAIL_VERIFICATION;
    final String token = TEST_TOKEN;

    when(tokenRepository.findByTokenAndType(eq(token), eq(type)))
      .thenReturn(Optional.empty());

    Assertions.assertThatThrownBy(() -> tokenService.validateToken(token, type, user))
      .isInstanceOf(InvalidTokenException.class);
  }

  @Test 
  void shouldReturn_tokenString_andMarkPreviousToken_onRenewToken() {
    final Token mockToken = createTestToken(TEST_TOKEN, TokenType.EMAIL_VERIFICATION);

    final User user = createTestUser(PETER_USERNAME);
    mockToken.setUser(user);

    final TokenType type = TokenType.EMAIL_VERIFICATION;

    when(tokenRepository.findByUserAndType(user, type))
      .thenReturn(List.of(mockToken));
    when(tokenRepository.save(any(Token.class))).thenAnswer((invocation) -> {
      Token tokenToSave = invocation.getArgument(0);
      tokenToSave.generateToken();
      return tokenToSave;
    });

    String result = tokenService.renewToken(user, type);

    verify(tokenRepository, times(1)).findByUserAndType(user, type);
    verify(tokenRepository, times(1)).saveAll(anyList());

    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result).hasSize(6);

    Assertions.assertThat(mockToken.getUsedAt()).isNotNull();
    Assertions.assertThat(mockToken.isUsed()).isTrue();
  }

  @Test 
  void shouldReturn_tokenString_withoutMarking_IfHasNoPreviousToken_onRenewToken() {
    final User user = createTestUser(PETER_USERNAME);
    final TokenType type = TokenType.EMAIL_VERIFICATION;

    when(tokenRepository.findByUserAndType(user, type))
      .thenReturn(List.of());

    when(tokenRepository.save(any(Token.class))).thenAnswer((invocation) -> {
      Token tokenToSave = invocation.getArgument(0);
      tokenToSave.generateToken();
      return tokenToSave;
    });

    String result = tokenService.renewToken(user, type);

    verify(tokenRepository, times(1)).findByUserAndType(user, type);
    verify(tokenRepository, times(0)).saveAll(anyList());

    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result).hasSize(6);
  }
}
