package com.example.cloud_share_api.auth;

import static com.example.cloud_share_api.TestUtils.PETER_USERNAME;
import static com.example.cloud_share_api.TestUtils.TEST_TOKEN;
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
import org.mockito.Mockito;
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

  private AuthService authService;

  @Captor
  private ArgumentCaptor<User> userCaptor;

  @Captor
  private ArgumentCaptor<Mail> mailCaptor;

  @BeforeEach
  void setup() {
    authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtProvider, tokenService, mailService);
    ReflectionTestUtils.setField(authService, "clientUrl", "http://localhost:4200");
  }

  @AfterEach
  void purge() {
    authService = null;
  }

  @Test
  void shouldCreateUser_andSendEmailVerificationMail_onRegisterUser() {
    final String encodedPassword = "encoded-password";
    final String token = "T0K3N0";

    final RegistrationRequest request = new RegistrationRequest("Peter", "Griffin", "peter@dev.in", "password");

    when(userRepository.findByEmail(request.email()))
      .thenReturn(Optional.empty());
    when(passwordEncoder.encode(request.password()))
      .thenReturn(encodedPassword);
    when(tokenService.generateToken(any(User.class), eq(TokenType.EMAIL_VERIFICATION)))
      .thenReturn(token);

    authService.registerUser(request);

    verify(userRepository, times(1)).findByEmail(request.email());
    verify(passwordEncoder, times(1)).encode(request.password());
    verify(userRepository, times(1)).save(userCaptor.capture());
    verify(tokenService, times(1)).generateToken(any(User.class), eq(TokenType.EMAIL_VERIFICATION));
    verify(mailService, times(1)).sendMail(mailCaptor.capture());

    User capture = userCaptor.getValue();
    Assertions.assertThat(capture).isNotNull();
    Assertions.assertThat(capture.getFirstname()).isEqualTo(request.firstname());
    Assertions.assertThat(capture.getLastname()).isEqualTo(request.lastname());
    Assertions.assertThat(capture.getEmail()).isEqualTo(request.email());
    Assertions.assertThat(capture.getPassword()).isEqualTo(encodedPassword);
    Assertions.assertThat(capture.isVerify()).isFalse();

    Mail mailCapture = mailCaptor.getValue();
    Assertions.assertThat(mailCapture).isNotNull();
    Assertions.assertThat(mailCapture.args().containsKey("username")).isTrue();
    Assertions.assertThat(mailCapture.args().containsKey("token")).isTrue();
    Assertions.assertThat(mailCapture.args().containsKey("client_url")).isTrue();
    Assertions.assertThat(mailCapture.args().get("username")).isEqualTo(request.firstname() + " " + request.lastname());
    Assertions.assertThat(mailCapture.args().get("token")).isEqualTo(token);
    Assertions.assertThat(mailCapture.args().get("client_url")).isEqualTo("http://localhost:4200/verify-email?email=" + request.email());
  }

  @Test
  void shouldThrowException_ifUserWithEmailAlreadyExists_onRegisterUser() {
    final User user = Mockito.mock(User.class);
    final RegistrationRequest request = new RegistrationRequest("Peter", "Griffin", "peter@dev.in", "password");

    when(userRepository.findByEmail(request.email()))
      .thenReturn(Optional.of(user));

    Assertions.assertThatThrownBy(() -> authService.registerUser(request))
      .isInstanceOf(DuplicateEntityException.class);
  }

  @Test
  void shouldAuthenticateUser_andReturnAuthResponse_onAuthenticateUser() {
    final String accessToken = "access-token";
    final String refreshToken = "refresh-token";
    final Authentication authentication = Mockito.mock(Authentication.class);
    final User user = Mockito.mock(User.class);

    final AuthRequest request = new AuthRequest("peter@dev.in", "password");

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
      .thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(user);
    when(user.isVerify()).thenReturn(true);
    when(user.getUsername()).thenReturn(request.email());
    when(jwtProvider.generateToken(eq(request.email())))
      .thenReturn(accessToken);
    when(jwtProvider.generateToken(eq(request.email()), eq(7*24*60*60)))
      .thenReturn(refreshToken);

    final AuthResponse result = authService.authenticateUser(request);

    verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    verify(user, times(1)).isVerify();
    verify(jwtProvider, times(1)).generateToken(eq(request.email()));
    verify(jwtProvider, times(1)).generateToken(eq(request.email()), eq(7*24*60*60));

    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result.accessToken()).isEqualTo(accessToken);
    Assertions.assertThat(result.refreshToken()).isEqualTo(refreshToken);
  }

  @Test
  void shouldThrowException_onAuthenticateUser_IfUserEmailIsNotVerified() {
    final Authentication authentication = Mockito.mock(Authentication.class);
    final User user = Mockito.mock(User.class);

    final AuthRequest request = new AuthRequest("peter@dev.in", "password");

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
      .thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(user);
    when(user.isVerify()).thenReturn(false);

    Assertions.assertThatThrownBy(() -> authService.authenticateUser(request))
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldThrowException_onAuthenticateUser_IfCredentialsAreInvalid() {
    final AuthRequest request = new AuthRequest("peter@dev.in", "password");

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
      .thenThrow(new BadCredentialsException("Invalid Credential"));

    Assertions.assertThatThrownBy(() -> authService.authenticateUser(request))
      .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void shouldVerifyUserEmail_andChangeUserVerifyState_toTrue_ifSuccessfull() {
    final String accessToken = "access-token";
    final String refreshToken = "refresh-token";
    final User mockUser = createTestUser(PETER_USERNAME);

    final String email = PETER_USERNAME;
    final String token = TEST_TOKEN;

    when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
    when(jwtProvider.generateToken(eq(email)))
      .thenReturn(accessToken);
    when(jwtProvider.generateToken(eq(email), eq(7*24*60*60)))
      .thenReturn(refreshToken);

    final AuthResponse result = authService.verifyEmail(email, token);

    verify(userRepository, times(1)).findByEmail(email);
    verify(userRepository, times(1)).save(any(User.class));
    verify(tokenService, times(1)).validateToken(eq(token), eq(TokenType.EMAIL_VERIFICATION), eq(mockUser));
    verify(jwtProvider, times(1)).generateToken(eq(email));
    verify(jwtProvider, times(1)).generateToken(eq(email), eq(7*24*60*60));

    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result.accessToken()).isEqualTo(accessToken);
    Assertions.assertThat(result.refreshToken()).isEqualTo(refreshToken);
    Assertions.assertThat(mockUser.isVerify()).isTrue();
  }

  @Test
  void shouldThrowException_onVerifyEmail_ifTokenIsInvalid() {
    final User mockUser = createTestUser(PETER_USERNAME);

    final String email = PETER_USERNAME;
    final String token = TEST_TOKEN;

    when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
    doThrow(new IllegalArgumentException("Invalid Token")).when(tokenService)
      .validateToken(token, TokenType.EMAIL_VERIFICATION, mockUser);

    Assertions.assertThatThrownBy(() -> authService.verifyEmail(email, token))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldThrowException_onVerifyEmail_ifInvalidUser() {
    final String email = PETER_USERNAME;
    final String token = TEST_TOKEN;

    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    Assertions.assertThatThrownBy(() -> authService.verifyEmail(email, token))
      .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void shouldCreateNewToken_andSendEmail_onRenewToken() {
    final User user = Mockito.mock(User.class);
    final String token = TEST_TOKEN;

    final String email = PETER_USERNAME;

    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(user.isVerify()).thenReturn(false);
    when(user.getFullname()).thenReturn("Peter Griffin");
    when(user.getEmail()).thenReturn(email);
    when(tokenService.renewToken(user, TokenType.EMAIL_VERIFICATION)).thenReturn(token);

    authService.renewToken(email);

    verify(userRepository, times(1)).findByEmail(email);
    verify(user, times(1)).isVerify();
    verify(tokenService, times(1)).renewToken(user, TokenType.EMAIL_VERIFICATION);
    verify(mailService, times(1)).sendMail(mailCaptor.capture());

    Mail mailCapture = mailCaptor.getValue();
    Assertions.assertThat(mailCapture).isNotNull();
    Assertions.assertThat(mailCapture.args().containsKey("username")).isTrue();
    Assertions.assertThat(mailCapture.args().containsKey("token")).isTrue();
    Assertions.assertThat(mailCapture.args().containsKey("client_url")).isTrue();
    Assertions.assertThat(mailCapture.args().get("username")).isEqualTo("Peter Griffin");
    Assertions.assertThat(mailCapture.args().get("token")).isEqualTo(token);
    Assertions.assertThat(mailCapture.args().get("client_url")).isEqualTo("http://localhost:4200/verify-email?email=" + email);
  }

  @Test
  void shouldThrowException_onRenewToken_ifUserIsAlreadyVerified() {
    final User user = Mockito.mock(User.class);

    final String email = PETER_USERNAME;

    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(user.isVerify()).thenReturn(true);

    Assertions.assertThatThrownBy(() -> authService.renewToken(email))
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldThrowException_onRenewToken_ifUserIsInvalid() {
    final String email = PETER_USERNAME;

    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    Assertions.assertThatThrownBy(() -> authService.renewToken(email))
      .isInstanceOf(EntityNotFoundException.class);
  }

  
}
 