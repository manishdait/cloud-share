package com.example.cloud_share_api.infrastructure.handler;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.cloud_share_api.infrastructure.exceptions.AccessDeniedException;
import com.example.cloud_share_api.infrastructure.exceptions.DuplicateEntityException;
import com.example.cloud_share_api.infrastructure.exceptions.ExpiredTokenException;
import com.example.cloud_share_api.infrastructure.exceptions.InsufficentCreditException;
import com.example.cloud_share_api.infrastructure.exceptions.InvalidTokenException;
import com.example.cloud_share_api.infrastructure.exceptions.TokenAlreadyUsedException;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler({
    IllegalArgumentException.class, 
    IllegalStateException.class, 
    InvalidTokenException.class, 
    TokenAlreadyUsedException.class, 
    ExpiredTokenException.class, 
    InsufficentCreditException.class
  })
  public ResponseEntity<ErrorResponse> handleBadRequest(Exception e, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
      new ErrorResponse(
        Instant.now(), 
        HttpStatus.BAD_REQUEST.value(),
        "Bad Request",
        e.getMessage(),
        request.getRequestURI()
      )
    );
  }

  @ExceptionHandler(DuplicateEntityException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgState(DuplicateEntityException e, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(
      new ErrorResponse(
        Instant.now(), 
        HttpStatus.CONFLICT.value(),
        "Conflict",
        e.getMessage(),
        request.getRequestURI()
      )
    );
  }

  @ExceptionHandler({
    BadCredentialsException.class, 
    AccessDeniedException.class
  })
  public ResponseEntity<ErrorResponse> handleBadCredentials(Exception e, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
      new ErrorResponse(
        Instant.now(), 
        HttpStatus.FORBIDDEN.value(), 
        "Forbidden", 
        e.getMessage(), 
        request.getRequestURI()
      )
    );
  } 

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException e, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
      new ErrorResponse(
        Instant.now(), 
        HttpStatus.NOT_FOUND.value(), 
        "Not Found", 
        e.getMessage(), 
        request.getRequestURI()
      )
    );
  } 

  @ExceptionHandler({
    JwtException.class, 
    ExpiredJwtException.class
  })
  public ResponseEntity<ErrorResponse> handleJwtException(Exception e, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
      new ErrorResponse(
        Instant.now(), 
        HttpStatus.UNAUTHORIZED.value(),
        "Unauthorize",
        e.getMessage(),
        request.getRequestURI()
      )
    );
  }

  @ExceptionHandler(Exception.class) 
  public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
    e.printStackTrace();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
      new ErrorResponse(
        Instant.now(), 
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "Something went wrong",
        e.getMessage(),
        request.getRequestURI()
      )
    );
  }
}
