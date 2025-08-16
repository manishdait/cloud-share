package com.example.cloud_share_api.payment;

import lombok.Getter;

public enum Plan {
  BASIC(100, 100, false),
  PREMIUM(500, 500, false),
  ULTIMATE(2500, 5000, true);

  @Getter
  private int amount;
  @Getter
  private int credits;
  @Getter
  private boolean isRecommended;

  Plan(int amount, int credits, boolean isRecommended) {
    this.amount = amount;
    this.credits = credits;
    this.isRecommended = isRecommended;
  }
}
