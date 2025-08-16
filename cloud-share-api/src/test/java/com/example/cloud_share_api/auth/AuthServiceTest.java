package com.example.cloud_share_api.auth;

import static com.example.cloud_share_api.TestUtils.createTestUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.cloud_share_api.email.Mail;
import com.example.cloud_share_api.email.MailService;
import com.example.cloud_share_api.infrastructure.exceptions.DuplicateEntityException;
import com.example.cloud_share_api.infrastructure.security.JwtProvider;
import com.example.cloud_share_api.token.TokenService;
import com.example.cloud_share_api.token.TokenType;
import com.example.cloud_share_api.user.User;
import com.example.cloud_share_api.user.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
  @Mock
  private UserRepository userRepository;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private AuthenticationManager authenticationManager;
  @Mock
  private JwtProvider jwtProvider;
  @Mock
  private TokenService tokenService;
  @Mock
  private MailService mailService;

  @Mock
  private Authentication authentication;

  private AuthService authService;

  @Captor
  private ArgumentCaptor<User> userCaptor;
  
  @Captor
  private ArgumentCaptor<Mail> mailCaptor;
  
  private User user;
  
  private static final String MOCK_TOKEN_STRING = "TOKEN0";

  @BeforeEach
  void setup() {
    user = createTestUser("user@test.in", "password");
    authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtProvider, tokenService, mailService);
    ReflectionTestUtils.setField(authService, "clientUrl", "http://localhost:4200");
  }

  @AfterEach
  void purge() {
    user = null;
    authService = null;
  }

  @Test
  void shouldCreateUserAndSendVerificationEmail_whenRegisteringUser() {
    final String encodedPassword = "encoded-password";

    final RegistrationRequest request = new RegistrationRequest("Peter", "Griffin", "peter@test.in", "password");
    when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
    when(passwordEncoder.encode(request.password())).thenReturn(encodedPassword);
    when(tokenService.generateToken(any(User.class), eq(TokenType.EMAIL_VERIFICATION))).thenReturn(MOCK_TOKEN_STRING);

    authService.registerUser(request);
    
    verify(userRepository, times(1)).save(userCaptor.capture());
    verify(passwordEncoder, times(1)).encode(request.password());
    verify(userRepository, times(1)).findByEmail(request.email());
    verify(tokenService, times(1)).generateToken(any(User.class), eq(TokenType.EMAIL_VERIFICATION));
    verify(mailService, times(1)).sendMail(mailCaptor.capture());
    
    User userCapture = userCaptor.getValue();
    Assertions.assertThat(userCapture).isNotNull();
    Assertions.assertThat(userCapture.getFirstname()).isEqualTo(request.firstname());
    Assertions.assertThat(userCapture.getLastname()).isEqualTo(request.lastname());
    Assertions.assertThat(userCapture.getEmail()).isEqualTo(request.email());
    Assertions.assertThat(userCapture.getCredit()).isEqualTo(5);
    Assertions.assertThat(userCapture.getPassword()).isEqualTo(encodedPassword);
    Assertions.assertThat(userCapture.isVerify()).isFalse();

    Mail mailCapture = mailCaptor.getValue();
    Assertions.assertThat(mailCapture).isNotNull();
    Assertions.assertThat(mailCapture.args().containsKey("username")).isTrue();
    Assertions.assertThat(mailCapture.args().containsKey("token")).isTrue();
    Assertions.assertThat(mailCapture.args().containsKey("client_url")).isTrue();
    Assertions.assertThat(mailCapture.args().get("username")).isEqualTo(request.firstname() + " " + request.lastname());
    Assertions.assertThat(mailCapture.args().get("token")).isEqualTo(MOCK_TOKEN_STRING);
    Assertions.assertThat(mailCapture.args().get("client_url")).isEqualTo("http://localhost:4200/verify-email?email=" + request.email());    
  }

  @Test
  void shouldThrowDuplicateEntityException_whenUserAlreadyExistsOnRegister() {
    final RegistrationRequest request = new RegistrationRequest("Peter", "Griffin", "user@test.in", "password");
    when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));

    Assertions.assertThatThrownBy(() -> authService.registerUser(request))
      .isInstanceOf(DuplicateEntityException.class);
    
    verify(userRepository, times(1)).findByEmail(request.email());
  }

  @Test
  void shouldAuthenticateUserAndReturnTokens_whenCredentialsAreValid() {
    user.setVerify(true);

    final String accessToken = "access-token";
    final String refreshToken = "refresh-token";

    final AuthRequest request = new AuthRequest("user@test.in", "password");
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(user);
    when(jwtProvider.generateToken(eq(request.email()))).thenReturn(accessToken);
    when(jwtProvider.generateToken(eq(request.email()), eq(7*24*60*60))).thenReturn(refreshToken);

    final AuthResponse result = authService.authenticateUser(request);

    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result.accessToken()).isEqualTo(accessToken);
    Assertions.assertThat(result.refreshToken()).isEqualTo(refreshToken);
    
    verify(jwtProvider, times(1)).generateToken(eq(request.email()));
    verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    verify(jwtProvider, times(1)).generateToken(eq(request.email()), eq(7*24*60*60));
  }

  @Test
  void shouldThrowException_whenAuthenticatingUnverifiedUser() {
    final AuthRequest request = new AuthRequest("user@test.in", "password");
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(user);

    Assertions.assertThatThrownBy(() -> authService.authenticateUser(request))
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldThrowBadCredentialsException_whenCredentialsAreInvalid() {
    final AuthRequest request = new AuthRequest("anotheruser@test.in", "password");
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
      .thenThrow(new BadCredentialsException("Invalid Credential"));

    Assertions.assertThatThrownBy(() -> authService.authenticateUser(request))
      .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void shouldVerifyUserEmailAndChangeStateToTrue_onSuccess() {
    final String accessToken = "access-token";
    final String refreshToken = "refresh-token";

    final String email = "user@test.in";
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(jwtProvider.generateToken(eq(email))).thenReturn(accessToken);
    when(jwtProvider.generateToken(eq(email), eq(7*24*60*60))).thenReturn(refreshToken);

    final AuthResponse result = authService.verifyEmail(email, MOCK_TOKEN_STRING);

    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result.accessToken()).isEqualTo(accessToken);
    Assertions.assertThat(result.refreshToken()).isEqualTo(refreshToken);
    Assertions.assertThat(user.isVerify()).isTrue();
    
    verify(userRepository, times(1)).findByEmail(email);
    verify(userRepository, times(1)).save(any(User.class));
    verify(tokenService, times(1)).validateToken(eq(MOCK_TOKEN_STRING), eq(TokenType.EMAIL_VERIFICATION), eq(user));
    verify(jwtProvider, times(1)).generateToken(eq(email));
    verify(jwtProvider, times(1)).generateToken(eq(email), eq(7*24*60*60));
  }

  @Test
  void shouldThrowException_whenVerifyingEmailWithInvalidToken() {
    final String email = "user@test.in";
    final String token = "000000";
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    doThrow(new IllegalArgumentException("Invalid Token")).when(tokenService)
      .validateToken(token, TokenType.EMAIL_VERIFICATION, user);

    Assertions.assertThatThrownBy(() -> authService.verifyEmail(email, token))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldThrowEntityNotFoundException_whenVerifyingEmailForNonExistentUser() {
    final String email = "anotheruser@test.in";
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    Assertions.assertThatThrownBy(() -> authService.verifyEmail(email, MOCK_TOKEN_STRING))
      .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void shouldCreateNewTokenAndSendEmail_whenRenewingToken() {
    final String email = "user@test.in";
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(tokenService.renewToken(user, TokenType.EMAIL_VERIFICATION)).thenReturn(MOCK_TOKEN_STRING);

    authService.renewToken(email);
    
    verify(mailService, times(1)).sendMail(mailCaptor.capture());
    verify(tokenService, times(1)).renewToken(user, TokenType.EMAIL_VERIFICATION);
    verify(userRepository, times(1)).findByEmail(email);

    Mail capture = mailCaptor.getValue();
    Assertions.assertThat(capture).isNotNull();
    Assertions.assertThat(capture.args().containsKey("username")).isTrue();
    Assertions.assertThat(capture.args().containsKey("token")).isTrue();
    Assertions.assertThat(capture.args().containsKey("client_url")).isTrue();
    Assertions.assertThat(capture.args().get("username")).isEqualTo("Test User");
    Assertions.assertThat(capture.args().get("token")).isEqualTo(MOCK_TOKEN_STRING);
    Assertions.assertThat(capture.args().get("client_url")).isEqualTo("http://localhost:4200/verify-email?email=" + email);    
  }

  @Test
  void shouldThrowIllegalStateException_whenRenewingTokenForVerifiedUser() {
    user.setVerify(true);

    final String email = "user@test.in";
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    
    Assertions.assertThatThrownBy(() -> authService.renewToken(email))
      .isInstanceOf(IllegalStateException.class);

    verify(userRepository, times(1)).findByEmail(email);
  }

  @Test
  void shouldThrowEntityNotFoundException_whenRenewingTokenForNonExistentUser() {
    final String email = "anotheruser@test.in";
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    Assertions.assertThatThrownBy(() -> authService.renewToken(email))
      .isInstanceOf(EntityNotFoundException.class);
    
    verify(userRepository, times(1)).findByEmail(email);
  }
  
}
 