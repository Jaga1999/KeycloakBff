package com.example.bff.common.api;

import java.time.Instant;

public record ApiResponse<T>(
        Instant timestamp,
        String requestId,
        int status,
        String message,
        T data,
        ErrorDetails error,
        long durationMs
) {

    public static <T> ApiResponse<T> success(String requestId, int status, String message, T data) {
        return new ApiResponse<>(Instant.now(), requestId, status, message, data, null, 0L);
    }

    public static <T> ApiResponse<T> failure(String requestId, int status, String message, ErrorDetails error) {
        return new ApiResponse<>(Instant.now(), requestId, status, message, null, error, 0L);
    }
}


