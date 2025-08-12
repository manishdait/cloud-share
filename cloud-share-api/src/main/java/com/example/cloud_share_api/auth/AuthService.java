package com.example.cloud_share_api.auth;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.cloud_share_api.email.Mail;
import com.example.cloud_share_api.email.MailService;
import com.example.cloud_share_api.email.MailTemplate;
import com.example.cloud_share_api.infrastructure.exceptions.DuplicateEntityException;
import com.example.cloud_share_api.infrastructure.exceptions.InvalidTokenException;
import com.example.cloud_share_api.infrastructure.security.JwtProvider;
import com.example.cloud_share_api.token.TokenService;
import com.example.cloud_share_api.token.TokenType;
import com.example.cloud_share_api.user.User;
import com.example.cloud_share_api.user.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;

  private final JwtProvider jwtProvider;
  private final TokenService tokenService;
  private final MailService mailService;

  @Value("${spring.application.client}")
  private String clientUrl;

  @Transactional
  public void registerUser(RegistrationRequest request) {
    userRepository.findByEmail(request.email()).ifPresent(u -> {
      throw new DuplicateEntityException("User alreay exist with the email");
    });
    
    User user = User.builder()
      .firstname(request.firstname())
      .lastname(request.lastname())
      .email(request.email())
      .credit(5)
      .password(passwordEncoder.encode(request.password()))
      .isVerify(false)
      .build();
    
    userRepository.save(user);

    String token = tokenService.generateToken(user, TokenType.EMAIL_VERIFICATION);
    sendMail(user, token);
  }

  public AuthResponse authenticateUser(AuthRequest request) {
    try {
      Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password())
      );
      User user = (User) authentication.getPrincipal();

      if (!user.isVerify()) {
        throw new IllegalStateException("User email address is not verified");
      }
  
      return createAuthResponse(user);
    } catch (BadCredentialsException e) {
      throw new BadCredentialsException("Invalid email or password");
    }
  }

  @Transactional
  public AuthResponse verifyEmail(String email, String token) {
    User user = userRepository.findByEmail(email).orElseThrow(
      () -> new EntityNotFoundException("User does not exist")
    );

    tokenService.validateToken(token, TokenType.EMAIL_VERIFICATION, user);

    user.setVerify(true);
    userRepository.save(user);

    return createAuthResponse(user);
  }

  public void renewToken(String email) {
    User user = userRepository.findByEmail(email).orElseThrow(
      () -> new EntityNotFoundException("User does not exist")
    );

    if (user.isVerify()) {
      throw new IllegalStateException("User is already verified");
    }

    String token = tokenService.renewToken(user, TokenType.EMAIL_VERIFICATION);
    sendMail(user, token); 
  }

  public AuthResponse refreshToken(HttpServletRequest request) {
    String token = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (token == null || !token.startsWith("Bearer ")) {
      throw new IllegalStateException("Authorization header missing");
    }

    token = token.substring(7);
    
    String username = jwtProvider.getUsername(token);
    User user = userRepository.findByEmail(username).orElseThrow(
      () -> new InvalidTokenException("Invalid Token")
    );
        
    if (!jwtProvider.validToken(user, token)) {
      throw new InvalidTokenException("Invalid Token");
    }

    String accessToken = jwtProvider.generateToken(user.getUsername());
    return new AuthResponse(accessToken, token);
  }

  private AuthResponse createAuthResponse(User user) {
    String accessToken = jwtProvider.generateToken(user.getUsername());
    String refreshToken = jwtProvider.generateToken(user.getUsername(), 7*24*60*60);

    return new AuthResponse(accessToken, refreshToken);
  }

  private void sendMail(User user, String token) {
    Mail mail = new Mail(
      user.getEmail(), 
      Map.of(
        "username", user.getFullname(), 
        "token", token, 
        "client_url", String.format("%s/verify-email?email=%s", clientUrl, user.getEmail())
      ), 
      MailTemplate.EMAIL_VERIFICATION
    );

    mailService.sendMail(mail);
  }
}
