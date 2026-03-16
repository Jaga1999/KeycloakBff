package com.example.bff.common.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID userId) {
        super("User not found with id: " + userId);
    }

    public UserNotFoundException(String identifier) {
        super("User not found with identifier: " + identifier);
    }
}

