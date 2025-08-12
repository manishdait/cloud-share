package com.example.cloud_share_api.infrastructure.config;

import java.nio.file.Path;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String uploadDir = Path.of("uploads").toAbsolutePath().toString();
    registry.addResourceHandler("/uploads/**").addResourceLocations("file:"+uploadDir+"/");
  }
}
