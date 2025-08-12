package com.example.cloud_share_api.email;

import java.util.Map;

public record Mail(String recipent, Map<String, Object> args, MailTemplate template) {
  
}
