package com.example.cloud_share_api.payment;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
  private final PaymentGatewayService paymentService;

  @PostMapping("/create-order")
  public ResponseEntity<PaymentDto> createOrder(@RequestParam(required = true) Plan plan, Authentication authentication) {
    PaymentDto paymentDto = paymentService.createOrder(plan, authentication);
    if (paymentDto.success()) {
      return ResponseEntity.status(HttpStatus.OK).body(paymentDto);
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentDto);
    }
  }

  @PostMapping("/verify")
  public ResponseEntity<PaymentDto> verifyPayment(@RequestBody PaymentVerificationDto request, Authentication authentication) {
    PaymentDto paymentDto = paymentService.verifyPayment(request, authentication);
    if (paymentDto.success()) {
      return ResponseEntity.status(HttpStatus.OK).body(paymentDto);
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentDto);
    }
  }

  @GetMapping("/transactions")
  public ResponseEntity<List<TransactionDto>> getUserTransactions(Authentication authentication) {
    return ResponseEntity.status(HttpStatus.OK).body(paymentService.getUserTransactions(authentication));
  }
}
