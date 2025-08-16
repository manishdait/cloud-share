package com.example.cloud_share_api.token;

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

  private User user;

  private static final String MOCK_TOKEN_STRING = "TOKEN0";

  @BeforeEach
  void setup() {
    user = createTestUser("user@test.in", "password");
    tokenService = new TokenService(tokenRepository);
  }

  @AfterEach
  void purge() {
    user = null;
    tokenService = null;
  }

  @Test
  void shouldGenerateAndReturnToken_whenCalled() {
    final TokenType type = TokenType.EMAIL_VERIFICATION;

    when(tokenRepository.save(any(Token.class))).thenAnswer((invocation) -> {
      Token tokenToSave = invocation.getArgument(0);
      tokenToSave.generateToken();
      return tokenToSave;
    });

    final String result = tokenService.generateToken(user, type);
    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result).hasSize(6);
   
    verify(tokenRepository, times(1)).save(tokenCaptor.capture());

    final Token capture = tokenCaptor.getValue();
    Assertions.assertThat(capture).isNotNull();
    Assertions.assertThat(capture.getType()).isEqualTo(type);
    Assertions.assertThat(capture.getUser()).isEqualTo(user);
    Assertions.assertThat(capture.isUsed()).isFalse();
    Assertions.assertThat(capture.getUsedAt()).isNull();    
  }

  @Test
  void shouldValidateTokenSuccessfully_whenTokenIsValid() {
    final Token mockToken = createTestToken(MOCK_TOKEN_STRING, TokenType.EMAIL_VERIFICATION);
    mockToken.setUser(user);

    final TokenType type = TokenType.EMAIL_VERIFICATION;

    when(tokenRepository.findByTokenAndType(eq(MOCK_TOKEN_STRING), eq(type))).thenReturn(Optional.of(mockToken));
    
    tokenService.validateToken(MOCK_TOKEN_STRING, type, user);
    
    Assertions.assertThat(mockToken.getUsedAt()).isNotNull();
    Assertions.assertThat(mockToken.isUsed()).isTrue();
    
    verify(tokenRepository, times(1)).findByTokenAndType(eq(MOCK_TOKEN_STRING), eq(type));
    verify(tokenRepository, times(1)).save(any(Token.class));
  }

  @Test
  void shouldThrowExpiredTokenException_whenTokenIsExpired() {
    final Token mockToken = Mockito.mock(Token.class);

    final TokenType type = TokenType.EMAIL_VERIFICATION;
    when(tokenRepository.findByTokenAndType(eq(MOCK_TOKEN_STRING), eq(type))).thenReturn(Optional.of(mockToken));
    when(mockToken.getUser()).thenReturn(user);
    when(mockToken.isUsed()).thenReturn(false);
    when(mockToken.isExpired()).thenReturn(true);

    Assertions.assertThatThrownBy(() -> tokenService.validateToken(MOCK_TOKEN_STRING, type, user))
      .isInstanceOf(ExpiredTokenException.class);

    verify(tokenRepository, times(1)).findByTokenAndType(eq(MOCK_TOKEN_STRING), eq(type));
  }

  @Test
  void  shouldThrowTokenAlreadyUsedException_whenTokenIsAlreadyUsed() {
    final Token mockToken = Mockito.mock(Token.class);

    final TokenType type = TokenType.EMAIL_VERIFICATION;
    when(tokenRepository.findByTokenAndType(eq(MOCK_TOKEN_STRING), eq(type))).thenReturn(Optional.of(mockToken));
    when(mockToken.getUser()).thenReturn(user);
    when(mockToken.isUsed()).thenReturn(true);

    Assertions.assertThatThrownBy(() -> tokenService.validateToken(MOCK_TOKEN_STRING, type, user))
      .isInstanceOf(TokenAlreadyUsedException.class);
    
    verify(tokenRepository, times(1)).findByTokenAndType(eq(MOCK_TOKEN_STRING), eq(type));
  }

  @Test
  void shouldThrowInvalidTokenException_whenTokenBelongsToDifferentUser() {
    final Token mockToken = Mockito.mock(Token.class);
    final User mockUser = createTestUser("anotheruser@test.in", "password");

    final TokenType type = TokenType.EMAIL_VERIFICATION;
    when(tokenRepository.findByTokenAndType(eq(MOCK_TOKEN_STRING), eq(type))).thenReturn(Optional.of(mockToken));
    when(mockToken.getUser()).thenReturn(mockUser);

    Assertions.assertThatThrownBy(() -> tokenService.validateToken(MOCK_TOKEN_STRING, type, user))
      .isInstanceOf(InvalidTokenException.class);

    verify(tokenRepository, times(1)).findByTokenAndType(eq(MOCK_TOKEN_STRING), eq(type));
  }

  @Test
  void shouldThrowInvalidException_whenTokenNotExists() {
    final TokenType type = TokenType.EMAIL_VERIFICATION;
    final String token = "000000";
    when(tokenRepository.findByTokenAndType(eq(token), eq(type))).thenReturn(Optional.empty());

    Assertions.assertThatThrownBy(() -> tokenService.validateToken(token, type, user))
      .isInstanceOf(InvalidTokenException.class);
    
    verify(tokenRepository, times(1)).findByTokenAndType(eq(token), eq(type));
  }

  @Test 
  void shouldGenerateNewToken_andInvalidateOldToken_onRenewTokenCall() {
    final Token mockToken = createTestToken(MOCK_TOKEN_STRING, TokenType.EMAIL_VERIFICATION);
    mockToken.setUser(user);

    final TokenType type = TokenType.EMAIL_VERIFICATION;
    when(tokenRepository.findByUserAndType(user, type)).thenReturn(List.of(mockToken));
    when(tokenRepository.save(any(Token.class))).thenAnswer((invocation) -> {
      Token tokenToSave = invocation.getArgument(0);
      tokenToSave.generateToken();
      return tokenToSave;
    });

    String result = tokenService.renewToken(user, type);
    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result).hasSize(6);
    
    verify(tokenRepository, times(1)).findByUserAndType(user, type);
    verify(tokenRepository, times(1)).saveAll(anyList());
  }

  @Test 
  void shouldGenerateNewTokenString_withoutPreviousTokenExists() {
    final TokenType type = TokenType.EMAIL_VERIFICATION;
    when(tokenRepository.findByUserAndType(user, type)).thenReturn(List.of());
    when(tokenRepository.save(any(Token.class))).thenAnswer((invocation) -> {
      Token tokenToSave = invocation.getArgument(0);
      tokenToSave.generateToken();
      return tokenToSave;
    });

    String result = tokenService.renewToken(user, type);
    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result).hasSize(6);
    
    verify(tokenRepository, times(0)).saveAll(anyList());
    verify(tokenRepository, times(1)).findByUserAndType(user, type);
  }
}
