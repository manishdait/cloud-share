package com.example.cloud_share_api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
  @Email(message = "User email must be a valid email address") 
  String email, 

  @NotBlank(message = "User password must not be blank") 
  @Size(min = 8, max = 16, message = "user password must be between 8 and 16 characters long")
  String password
) {
  
}
