package com.example.cloud_share_api.infrastructure.exceptions;

public class TokenAlreadyUsedException extends RuntimeException {
  public TokenAlreadyUsedException(String message) {
    super(message);
  }
}
