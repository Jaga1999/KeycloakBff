package com.example.bff.auth.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class Session implements Serializable {

    private UUID sessionId;
    private UUID userId;
    private String keycloakUserId;
    private Set<String> roles;
    private String accessToken;
    private Instant accessTokenExpiresAt;
    private String refreshToken;
    private Instant refreshTokenExpiresAt;
    private Instant issuedAt;
    private Map<String, Object> claims;
    private boolean rememberMe;
}
