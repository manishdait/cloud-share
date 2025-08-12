package com.example.cloud_share_api.payment;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentGatewayService {
  @Value("${razorpay.secret-key}")
  private String secretKey;
  @Value("${razorpay.api-key}")
  private String apiKey;

  public PaymentDto createOrder(Integer amount, String currency) {
    try {
      RazorpayClient client = new RazorpayClient(apiKey, secretKey);
      JSONObject orderRequest = new JSONObject();
      orderRequest.put("amount", amount*100);
      orderRequest.put("currency", currency);

      Order order = client.orders.create(orderRequest);
      return new PaymentDto(order.get("id"), true, "Order created successfully");
    } catch (RazorpayException e) {
      e.printStackTrace();
      return new PaymentDto("", true, "Error creating Order");
    }
  }
}