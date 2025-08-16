package com.example.cloud_share_api.payment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.cloud_share_api.user.User;


public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
  Optional<PaymentTransaction> findByOrderId(String orderId);
  List<PaymentTransaction> findByUser(User user);
}
