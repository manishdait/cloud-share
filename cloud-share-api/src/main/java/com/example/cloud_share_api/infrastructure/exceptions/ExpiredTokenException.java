package com.example.cloud_share_api.infrastructure.exceptions;

public class ExpiredTokenException extends RuntimeException {
  public ExpiredTokenException(String message) {
    super(message);
  }
}
