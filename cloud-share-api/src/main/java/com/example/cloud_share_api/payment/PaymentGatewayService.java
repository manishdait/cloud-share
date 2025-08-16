package com.example.cloud_share_api.payment;

import java.time.Instant;
import java.util.List;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.example.cloud_share_api.user.User;
import com.example.cloud_share_api.user.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentGatewayService {
  private final PaymentTransactionRepository paymentTransactionRepository;
  private final UserRepository userRepository;

  @Value("${razorpay.secret-key}")
  private String secretKey;
  @Value("${razorpay.api-key}")
  private String apiKey;

  @Transactional
  public PaymentDto createOrder(Plan plan, Authentication authentication) {
    User user = (User) authentication.getPrincipal();

    try {
      RazorpayClient client = new RazorpayClient(apiKey, secretKey);
      JSONObject orderRequest = new JSONObject();
      orderRequest.put("amount", plan.getAmount()*100);
      orderRequest.put("currency", "INR");

      Order order = client.orders.create(orderRequest);
      PaymentTransaction paymentTransaction = PaymentTransaction.builder()
        .amount(plan.getAmount())
        .credits(plan.getCredits())
        .orderId(order.get("id"))
        .user(user)
        .plan(plan)
        .status(Status.PROCESSING)
        .transactionDate(Instant.now())
        .build();

      paymentTransactionRepository.save(paymentTransaction);
      
      return new PaymentDto(order.get("id"), true, "Order created successfully");
    } catch (RazorpayException e) {
      e.printStackTrace();
      return new PaymentDto("", true, "Error creating Order");
    }
  }

  @Transactional
  public PaymentDto verifyPayment(PaymentVerificationDto request, Authentication authentication) {
    String paylod = request.orederId() + "|" + request.paymentId();

    try {
      boolean isVerified = Utils.verifySignature(paylod, request.signature(), secretKey);

      PaymentTransaction paymentTransaction = paymentTransactionRepository.findByOrderId(request.orederId()).orElseThrow(
        () -> new EntityNotFoundException("Payment transaction not fond")
      );

      paymentTransaction.setPaymentId(request.paymentId());
      if (!isVerified) {
        paymentTransaction.setStatus(Status.FAIL);
      } else{ 
        paymentTransaction.setStatus(Status.SUCCESS);
        
        User user = paymentTransaction.getUser();
        user.setCredit(user.getCredit() + paymentTransaction.getCredits());
        userRepository.save(user);
      }

      paymentTransactionRepository.save(paymentTransaction);
      if (isVerified) {
        return new PaymentDto("", true, "Payment verified successfully");
      } else {
        return new PaymentDto("", false, "Invalid payment signature");
      }
    } catch (Exception e) {
      e.printStackTrace();
      return new PaymentDto("", false, "Error verifying payment");
    }
  }

  public List<TransactionDto> getUserTransactions(Authentication authentication) {
    User user = (User) authentication.getPrincipal();

    return paymentTransactionRepository.findByUser(user)
      .stream()
      .filter(t -> !t.getStatus().equals(Status.PROCESSING))
      .map(t -> new TransactionDto(t.getPaymentId(), t.getPlan(), t.getAmount(), t.getStatus(), t.getTransactionDate()))
      .toList();
  }
}