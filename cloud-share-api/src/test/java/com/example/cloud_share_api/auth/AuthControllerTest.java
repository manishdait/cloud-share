package com.example.cloud_share_api.auth;

import static com.example.cloud_share_api.TestUtils.TEST_TOKEN;
import static com.example.cloud_share_api.TestUtils.createTestToken;
import static com.example.cloud_share_api.TestUtils.createTestUser;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.example.cloud_share_api.infrastructure.handler.ErrorResponse;
import com.example.cloud_share_api.token.Token;
import com.example.cloud_share_api.token.TokenRepository;
import com.example.cloud_share_api.token.TokenType;
import com.example.cloud_share_api.user.User;
import com.example.cloud_share_api.user.UserRepository;

@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "test")
public class AuthControllerTest {
  @Container
  @ServiceConnection
  private static PostgreSQLContainer<?> psqlContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:alpine"));
  
  @SuppressWarnings("resource")
  @Container
  private static GenericContainer<?> maildevContainer = new GenericContainer<>(DockerImageName.parse("maildev/maildev"))
    .withExposedPorts(1025, 1080);

  @DynamicPropertySource
  static void configureMaildevProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.mail.host", () -> maildevContainer.getHost());
    registry.add("spring.mail.port", () -> maildevContainer.getMappedPort(1025));
  }

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TokenRepository tokenRepository;
  
  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private TestRestTemplate restTemplate;

  private final String BASE_URL = "/api/v1/auth";

  @BeforeEach
  void setup() {
    tokenRepository.deleteAll();
    userRepository.deleteAll();
  }

  @AfterEach
  void purge() {
    tokenRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void canEstablishConnection() {
    Assertions.assertThat(psqlContainer.isCreated()).isTrue();
    Assertions.assertThat(psqlContainer.isRunning()).isTrue();

    Assertions.assertThat(maildevContainer.isCreated()).isTrue();
    Assertions.assertThat(maildevContainer.isRunning()).isTrue();
  }

  @Test 
  void shouldRegisterNewUser() {
    final RegistrationRequest request = new RegistrationRequest("Peter", "Griffin", "peter@dev.com", "password");

    ResponseEntity<Map<String, Boolean>> result =  restTemplate.exchange(
      BASE_URL + "/sign-up",
      HttpMethod.POST,
      new HttpEntity<>(request, null),
      new ParameterizedTypeReference<Map<String, Boolean>>(){}
    );

    Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  @Test 
  void shouldReturnConflict_onRegisterNewUser_ifUserAlreadyExists() {
    final RegistrationRequest request1 = new RegistrationRequest("Peter", "Griffin", "peter@dev.com", "password");

    ResponseEntity<Map<String, Boolean>> signUpResult =  restTemplate.exchange(
      BASE_URL + "/sign-up",
      HttpMethod.POST,
      new HttpEntity<>(request1, null),
      new ParameterizedTypeReference<Map<String, Boolean>>(){}
    );

    Assertions.assertThat(signUpResult.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    final RegistrationRequest request2 = new RegistrationRequest("Louis", "Griffin", "peter@dev.com", "password");

    ResponseEntity<ErrorResponse> result =  restTemplate.exchange(
      BASE_URL + "/sign-up",
      HttpMethod.POST,
      new HttpEntity<>(request2, null),
      ErrorResponse.class
    );

    Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test 
  void shouldVerifyUserEmail() {
    User user = createTestUser("peter@dev.in", passwordEncoder.encode("password"));
    user = userRepository.save(user);

    String token = TEST_TOKEN;
    Token verificationToken = createTestToken(token, TokenType.EMAIL_VERIFICATION);
    verificationToken.setUser(user);

    tokenRepository.save(verificationToken);

    ResponseEntity<AuthResponse> result =  restTemplate.exchange(
      BASE_URL + "/verify-email?email=peter@dev.in&token=" + token,
      HttpMethod.POST,
      new HttpEntity<>(null),
      AuthResponse.class
    );

    Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    Assertions.assertThat(result.getBody().accessToken()).isNotBlank();
    Assertions.assertThat(result.getBody().refreshToken()).isNotBlank();
  }

  @Test 
  void shouldReturn_badRequest_onVerifyUserEmail_ifInvalidToken() {
    User user = createTestUser("peter@dev.in", passwordEncoder.encode("password"));
    user = userRepository.save(user);

    Token verificationToken = createTestToken(TEST_TOKEN, TokenType.EMAIL_VERIFICATION);
    verificationToken.setUser(user);

    tokenRepository.save(verificationToken);

    ResponseEntity<ErrorResponse> result =  restTemplate.exchange(
      BASE_URL + "/verify-email?email=peter@dev.in&token=000000",
      HttpMethod.POST,
      new HttpEntity<>(null),
      ErrorResponse.class
    );

    Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test 
  void shouldReturn_notFound_onVerifyUserEmail_ifInvalidUser() {
    User user = createTestUser("peter@dev.in", passwordEncoder.encode("password"));
    user = userRepository.save(user);

    Token verificationToken = createTestToken(TEST_TOKEN, TokenType.EMAIL_VERIFICATION);
    verificationToken.setUser(user);

    tokenRepository.save(verificationToken);

    ResponseEntity<ErrorResponse> result =  restTemplate.exchange(
      BASE_URL + "/verify-email?email=louis@dev.in&token=000000",
      HttpMethod.POST,
      new HttpEntity<>(null),
      ErrorResponse.class
    );

    Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldRenewToken() {
    User user = createTestUser("peter@dev.in", passwordEncoder.encode("password"));
    user = userRepository.save(user);

    Token verificationToken = createTestToken(TEST_TOKEN, TokenType.EMAIL_VERIFICATION);
    verificationToken.setUser(user);

    tokenRepository.save(verificationToken);

    ResponseEntity<Map<String, Boolean>> result =  restTemplate.exchange(
      BASE_URL + "/renew-token?email=peter@dev.in",
      HttpMethod.POST,
      new HttpEntity<>(null),
      new ParameterizedTypeReference<Map<String, Boolean>>() {}
    );

    Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void shouldReturn_badRequest_onRenewToken_ifUserIsAlreadyVerify() {
    User user = createTestUser("peter@dev.in", passwordEncoder.encode("password"));
    user.setVerify(true);
    user = userRepository.save(user);

    ResponseEntity<ErrorResponse> result =  restTemplate.exchange(
      BASE_URL + "/renew-token?email=peter@dev.in",
      HttpMethod.POST,
      new HttpEntity<>(null),
      ErrorResponse.class
    );

    Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldReturn_notFound_onRenewToken_ifUserIsInvalid() {
    User user = createTestUser("peter@dev.in", passwordEncoder.encode("password"));
    user = userRepository.save(user);

    Token verificationToken = createTestToken(TEST_TOKEN, TokenType.EMAIL_VERIFICATION);
    verificationToken.setUser(user);

    tokenRepository.save(verificationToken);

    ResponseEntity<ErrorResponse> result =  restTemplate.exchange(
      BASE_URL + "/renew-token?email=louis@dev.in",
      HttpMethod.POST,
      new HttpEntity<>(null),
      ErrorResponse.class
    );

    Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldAuthenticateUser() {
    User user = createTestUser("peter@dev.in", passwordEncoder.encode("password"));
    user.setVerify(true);
    user = userRepository.save(user);

    final AuthRequest request = new AuthRequest("peter@dev.in", "password");

    ResponseEntity<AuthResponse> result =  restTemplate.exchange(
      BASE_URL + "/login",
      HttpMethod.POST,
      new HttpEntity<>(request, null),
      AuthResponse.class
    );

    Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    Assertions.assertThat(result.getBody().accessToken()).isNotBlank();
    Assertions.assertThat(result.getBody().refreshToken()).isNotBlank();
  }

  @Test
  void shouldThrow_badRequest_whenAuthenticateUser_isNotVerified() {
    User user = createTestUser("peter@dev.in", passwordEncoder.encode("password"));
    user = userRepository.save(user);

    final AuthRequest request = new AuthRequest("peter@dev.in", "password");

    ResponseEntity<ErrorResponse> result =  restTemplate.exchange(
      BASE_URL + "/login",
      HttpMethod.POST,
      new HttpEntity<>(request, null),
      ErrorResponse.class
    );

    Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldThrow_forbidden_whenAuthenticateUser_onInvalidCred() {
    User user = createTestUser("peter@dev.in", passwordEncoder.encode("password"));
    user = userRepository.save(user);

    final AuthRequest request = new AuthRequest("louis@dev.in", "password");

    ResponseEntity<ErrorResponse> result =  restTemplate.exchange(
      BASE_URL + "/login",
      HttpMethod.POST,
      new HttpEntity<>(request, null),
      ErrorResponse.class
    );

    Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void shouldRefreshJwtToken() {
    User user = createTestUser("peter@dev.in", passwordEncoder.encode("password"));
    user.setVerify(true);
    user = userRepository.save(user);
    
    final AuthRequest request = new AuthRequest("peter@dev.in", "password");

    AuthResponse auth =  restTemplate.exchange(
      BASE_URL + "/login",
      HttpMethod.POST,
      new HttpEntity<>(request, null),
      AuthResponse.class
    ).getBody();

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + auth.refreshToken());

    ResponseEntity<AuthResponse> result =  restTemplate.exchange(
      BASE_URL + "/refresh",
      HttpMethod.POST,
      new HttpEntity<>(request, headers),
      AuthResponse.class
    );

    Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    Assertions.assertThat(result.getBody().refreshToken()).isEqualTo(auth.refreshToken());
    Assertions.assertThat(result.getBody().accessToken()).isNotBlank();
  }
}
