package com.example.cloud_share_api.infrastructure.exceptions;

public class AccessDeniedException extends RuntimeException { 
  public AccessDeniedException() {
    super("Unauthorized access permission to access this resource is denied");
  }
}
