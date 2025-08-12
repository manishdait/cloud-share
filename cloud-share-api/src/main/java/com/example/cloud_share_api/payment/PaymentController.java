package com.example.cloud_share_api.payment;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
  private final PaymentGatewayService paymentService;

  @PostMapping("/create-order")
  public ResponseEntity<PaymentDto> createOrder(@RequestParam(required = true) Integer amount, @RequestParam(required = true) String currency) {
    PaymentDto paymentDto = paymentService.createOrder(amount, currency);
    if (paymentDto.success()) {
      return ResponseEntity.status(HttpStatus.OK).body(paymentDto);
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentDto);
    }
  }
}
