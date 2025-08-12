package com.example.cloud_share_api.auth;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;

  @PostMapping("/sign-up")
  public ResponseEntity<Map<String, Boolean>> registerUser(@RequestBody RegistrationRequest request) {
    authService.registerUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("created", true));
  }
  
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> authenticateUser(@RequestBody AuthRequest request) {
    return ResponseEntity.status(HttpStatus.OK).body(authService.authenticateUser(request));
  }

  @PostMapping("/verify-email")
  public ResponseEntity<AuthResponse> verifyEmail(@RequestParam(required = true) String email, @RequestParam(required = true) String token) {
    return ResponseEntity.status(HttpStatus.OK).body(authService.verifyEmail(email, token));
  }

  @PostMapping("/renew-token")
  public ResponseEntity<Map<String, Boolean>> renewToken(@RequestParam(required = true) String email) {
    authService.renewToken(email);
    return ResponseEntity.status(HttpStatus.OK).body(Map.of("resend-token", true));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refreshToken(HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.OK).body(authService.refreshToken(request));
  }
}
