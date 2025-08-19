package com.example.cloud_share_api.infrastructure.security;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.cloud_share_api.auth.ResetPasswordRequest;
import com.example.cloud_share_api.email.Mail;
import com.example.cloud_share_api.email.MailService;
import com.example.cloud_share_api.email.MailTemplate;
import com.example.cloud_share_api.token.TokenService;
import com.example.cloud_share_api.token.TokenType;
import com.example.cloud_share_api.user.User;
import com.example.cloud_share_api.user.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordService {
  private final UserRepository userRepository;
  private final TokenService tokenService;
  private final MailService mailService;

  private final PasswordEncoder passwordEncoder;

  @Value("${spring.application.client}")
  private String clientUrl;

  public void forgotPassword(String email) {
    User user = findUserByEmail(email);
    String token = tokenService.generateToken(user, TokenType.RESET_PASSWORD);
    sendMail(user, token);   
  }

  @Transactional
  public void resetPasswordByEmail(String email, ResetPasswordRequest request) {
    User user = findUserByEmail(email);
    tokenService.validateToken(request.token(), TokenType.RESET_PASSWORD, user);

    user.setPassword(passwordEncoder.encode(request.password()));
    userRepository.save(user);
  }

  @Transactional
  public void resetCurrentUserPassword(Authentication authentication, ResetPasswordRequest request) {
    User user = (User) authentication.getPrincipal();
    user.setPassword(passwordEncoder.encode(request.password()));
    userRepository.save(user);
  }

  public void renewToken(String email) {
    User user = findUserByEmail(email);
    String token = tokenService.renewToken(user, TokenType.RESET_PASSWORD);
    sendMail(user, token); 
  }


  private User findUserByEmail(String email) {
    return userRepository.findByEmail(email).orElseThrow(
      () -> new EntityNotFoundException("User not found")
    );
  }

  private void sendMail(User user, String token) {
    Mail mail = new Mail(
      user.getEmail(), 
      Map.of(
        "username", user.getFullname(), 
        "token", token, 
        "client_url", String.format("%s/reset-password?email=%s", clientUrl, user.getEmail())
      ), 
      MailTemplate.RESET_PASSWORD
    );

    mailService.sendMail(mail);
  }
}
