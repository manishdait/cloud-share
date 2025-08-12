package com.example.cloud_share_api.infrastructure.exceptions;

public class DuplicateEntityException extends RuntimeException { 
  public DuplicateEntityException() {
    super("A resource with the same identifier already exists");
  }

  public DuplicateEntityException(String message) {
    super();
  }
}
