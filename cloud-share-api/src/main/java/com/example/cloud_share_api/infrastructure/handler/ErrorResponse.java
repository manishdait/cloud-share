package com.example.cloud_share_api.infrastructure.handler;

import java.time.Instant;

public record ErrorResponse(
  Instant timestamp,
  Integer status,
  String error,
  String message,
  String path
) {

}
