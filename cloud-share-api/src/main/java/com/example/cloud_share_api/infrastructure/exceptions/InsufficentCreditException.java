package com.example.cloud_share_api.infrastructure.exceptions;

public class InsufficentCreditException extends RuntimeException {
  public InsufficentCreditException() {
    super("User has insufficent credit to perform actions");
  }
}
