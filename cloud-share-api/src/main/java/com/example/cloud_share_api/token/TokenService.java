package com.example.cloud_share_api.token;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.cloud_share_api.infrastructure.exceptions.ExpiredTokenException;
import com.example.cloud_share_api.infrastructure.exceptions.InvalidTokenException;
import com.example.cloud_share_api.infrastructure.exceptions.TokenAlreadyUsedException;
import com.example.cloud_share_api.user.User;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenService {
  private final TokenRepository tokenRepository;

  @Transactional
  public String generateToken(User user, TokenType type) {
    Token token = Token.builder()
      .user(user)
      .type(type)
      .expiration(Instant.now().plusSeconds(15*60))
      .isUsed(false)
      .build();

    tokenRepository.save(token);
    return token.getToken();
  }

  @Transactional
  public void validateToken(String _token, TokenType type, User user) {
    Token token = tokenRepository.findByTokenAndType(_token, type).orElseThrow(
      () -> new InvalidTokenException("Invalid verification token")
    );

    if (!token.getUser().getUsername().equals(user.getUsername())) {
      throw new InvalidTokenException("Invalid verification token");
    }

    if (token.isUsed()) {
      throw new TokenAlreadyUsedException("Token has already been used");
    }

    if (token.isExpired()) {
      throw new ExpiredTokenException("Token ihas expired");
    }

    token.setUsedAt(Instant.now());
    token.setUsed(true);
    tokenRepository.save(token);
  }

  @Transactional
  public String renewToken(User user, TokenType type) {
    List<Token> tokens = tokenRepository.findByUserAndType(user, type);
    
    if (!tokens.isEmpty()) {
      for (Token token : tokens) {
        token.setUsedAt(Instant.now());
        token.setUsed(true);
      }

      tokenRepository.saveAll(tokens);
    }

    return generateToken(user, type);
  }
}
