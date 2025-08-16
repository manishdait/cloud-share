package com.example.cloud_share_api.payment;

import java.time.Instant;

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
@Table(name = "payment_transaction")
public class PaymentTransaction {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_transaction_seq_generator")
  @SequenceGenerator(name = "payment_transaction_seq_generator", sequenceName = "payment_transaction_seq", allocationSize = 1, initialValue = 101)
  @Column(name = "id")
  private Long id;

  @Column(name = "amount", nullable = false)
  private int amount;
  
  @Column(name = "credits", nullable = false)
  private int credits;

  @Column(name = "order_id", unique = true, nullable = false)
  private String orderId;

  @Column(name = "payment_id", unique = true)
  private String paymentId;

  @Enumerated(value = EnumType.STRING)
  @Column(name = "plan", nullable = false)
  private Plan plan;

  @Column(name = "transaction_date", updatable = false, nullable = false)
  private Instant transactionDate;

  @Enumerated(value = EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
}
