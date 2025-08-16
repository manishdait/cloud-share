package com.example.cloud_share_api.infrastructure.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
  @Value("${spring.application.file.upload-dir}")
  private String dir;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String uploadDir = Path.of(dir).toAbsolutePath().toString();
    registry.addResourceHandler("/uploads/**").addResourceLocations("file:"+uploadDir+"/");
  }
}
