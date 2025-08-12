package com.example.cloud_share_api.token;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Random;

import com.example.cloud_share_api.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "token")
public class Token { 
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "token_seq_generator")
  @SequenceGenerator(name = "token_seq_generator", sequenceName = "token_seq", allocationSize = 1, initialValue = 101)
  @Column(name = "id")
  private Long id;

  @Column(name = "token", unique = true, nullable = false)
  private String token;

  @Enumerated(value = EnumType.STRING)
  @Column(name = "type", nullable = false)
  private TokenType type;

  @Column(name = "expiration", nullable = false)
  private Instant expiration;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "is_used", nullable = false)
  private boolean isUsed;

  @Column(name = "used_at", nullable = true)
  private Instant usedAt;

  public boolean isExpired() {
    return this.expiration.isBefore(Instant.now());
  }

  @PrePersist
  public void generateToken() {
    if (token == null) {
      String charSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
      StringBuilder sb = new StringBuilder();

      Random random = new SecureRandom();
      for (int i = 0; i < 6; i++) {
        int index = random.nextInt(charSet.length());
        sb.append(charSet.charAt(index));
      }
      this.token = sb.toString();
    }
  }
}
