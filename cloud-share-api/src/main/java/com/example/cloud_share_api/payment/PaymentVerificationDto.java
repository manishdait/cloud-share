package com.example.cloud_share_api.payment;

public record PaymentVerificationDto(String orederId, String paymentId, String signature) {
  
}
