package com.example.bff.common.exception;

import com.example.bff.common.api.ApiResponse;
import com.example.bff.common.api.ErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        ErrorDetails error = new ErrorDetails("USER_ALREADY_EXISTS", ex.getMessage(), null);
        ApiResponse<Void> body = ApiResponse.failure(null, HttpStatus.CONFLICT.value(), ex.getMessage(), error);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationFailed(AuthenticationFailedException ex) {
        ErrorDetails error = new ErrorDetails("AUTHENTICATION_FAILED", ex.getMessage(), null);
        ApiResponse<Void> body = ApiResponse.failure(null, HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), error);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException ex) {
        ErrorDetails error = new ErrorDetails("USER_NOT_FOUND", ex.getMessage(), null);
        ApiResponse<Void> body = ApiResponse.failure(null, HttpStatus.NOT_FOUND.value(), ex.getMessage(), error);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(TodoNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTodoNotFound(TodoNotFoundException ex) {
        ErrorDetails error = new ErrorDetails("TODO_NOT_FOUND", ex.getMessage(), null);
        ApiResponse<Void> body = ApiResponse.failure(null, HttpStatus.NOT_FOUND.value(), ex.getMessage(), error);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSessionNotFound(SessionNotFoundException ex) {
        ErrorDetails error = new ErrorDetails("SESSION_NOT_FOUND", ex.getMessage(), null);
        ApiResponse<Void> body = ApiResponse.failure(null, HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), error);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenRefresh(TokenRefreshException ex) {
        ErrorDetails error = new ErrorDetails("TOKEN_REFRESH_FAILED", ex.getMessage(), null);
        ApiResponse<Void> body = ApiResponse.failure(null, HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), error);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(KeycloakAdminException.class)
    public ResponseEntity<ApiResponse<Void>> handleKeycloakAdmin(KeycloakAdminException ex) {
        ErrorDetails error = new ErrorDetails("KEYCLOAK_ADMIN_ERROR", ex.getMessage(), null);
        ApiResponse<Void> body = ApiResponse.failure(null, HttpStatus.BAD_GATEWAY.value(), ex.getMessage(), error);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        ErrorDetails error = new ErrorDetails("ACCESS_DENIED", "Access denied", null);
        ApiResponse<Void> body = ApiResponse.failure(null, HttpStatus.FORBIDDEN.value(), "Access denied", error);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ErrorDetails error = new ErrorDetails("VALIDATION_ERROR", "Validation failed", validationErrors);
        ApiResponse<Void> body = ApiResponse.failure(null, HttpStatus.BAD_REQUEST.value(), "Validation failed", error);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unexpected error occurred", ex);
        ErrorDetails error = new ErrorDetails("INTERNAL_ERROR", ex.getMessage(), null);
        ApiResponse<Void> body = ApiResponse.failure(null, HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unexpected error", error);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
