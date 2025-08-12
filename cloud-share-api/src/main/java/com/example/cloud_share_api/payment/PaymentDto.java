package com.example.cloud_share_api.payment;

public record PaymentDto(String orderId, boolean success, String message) {
  
}
