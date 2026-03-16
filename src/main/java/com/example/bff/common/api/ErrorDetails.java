package com.example.bff.common.api;

public record ErrorDetails(
        String code,
        String message,
        Object details
) {
}

