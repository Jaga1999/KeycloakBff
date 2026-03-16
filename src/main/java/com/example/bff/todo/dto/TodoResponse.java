package com.example.bff.todo.dto;

import java.time.Instant;
import java.util.UUID;

public record TodoResponse(
        UUID id,
        String title,
        String description,
        UUID ownerId,
        UUID createdBy,
        UUID updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
}

