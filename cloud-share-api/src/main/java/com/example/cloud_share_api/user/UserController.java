package com.example.cloud_share_api.user;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.cloud_share_api.auth.ResetPasswordRequest;
import com.example.cloud_share_api.infrastructure.security.PasswordService;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;
  private final PasswordService passwordService;

  @GetMapping("/me")
  public ResponseEntity<UserDto> getUserSummary(Authentication authentication) {
    return ResponseEntity.status(HttpStatus.OK).body(userService.getUserSummary(authentication));
  }

  @PatchMapping("/password/me")
  public ResponseEntity<Map<String,Boolean>> resetCurrentUserPassword(@RequestBody ResetPasswordRequest request, Authentication authentication) {
    passwordService.resetCurrentUserPassword(authentication, request);
    return ResponseEntity.status(HttpStatus.OK).body(Map.of("resert-password", true));
  }

  @PatchMapping("/reset-password/{email}")
  public ResponseEntity<Map<String,Boolean>> resetUserPassword(@RequestBody ResetPasswordRequest request, @PathVariable String email) {
    passwordService.resetPasswordByEmail(email, request);
    return ResponseEntity.status(HttpStatus.OK).body(Map.of("resert-password", true));
  }
}
