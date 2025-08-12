package com.example.cloud_share_api.file;

import java.time.Instant;


public record FileDto(
  String uuid,
  String name,
  String type,
  Long size,
  Boolean isPublic,
  Instant uploadedAt
) {
  
}
