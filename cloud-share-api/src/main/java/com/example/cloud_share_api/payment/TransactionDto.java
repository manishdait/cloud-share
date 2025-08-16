package com.example.cloud_share_api.payment;

import java.time.Instant;

public record TransactionDto(String id, Plan plan, int amount, Status status, Instant timestamp) {
  
}
