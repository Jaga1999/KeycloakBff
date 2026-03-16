package com.example.bff.auth.dto;

import java.util.Set;
import java.util.UUID;

public record MeResponse(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        Set<String> roles
) {
}

