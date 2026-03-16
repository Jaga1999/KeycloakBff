package com.example.bff.auth.dto;

import java.util.Set;
import java.util.UUID;

public record LoginResponse(
        String message,
        UUID userId,
        String username,
        String email,
        Set<String> roles
) {
}

