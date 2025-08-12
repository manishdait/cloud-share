package com.example.cloud_share_api.token;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cloud_share_api.user.User;

public interface TokenRepository extends JpaRepository<Token, Long> {
  Optional<Token> findByTokenAndType(String token, TokenType type);
  List<Token> findByUserAndType(User user, TokenType type);
}
