package com.example.bff.auth.service;

import com.example.bff.auth.dto.ForgotPasswordRequest;
import com.example.bff.auth.dto.LoginRequest;
import com.example.bff.auth.dto.LoginResponse;
import com.example.bff.auth.dto.MeResponse;
import com.example.bff.auth.dto.RegisterRequest;
import com.example.bff.auth.dto.RegisterResponse;
import com.example.bff.auth.model.Session;
import com.example.bff.common.exception.AuthenticationFailedException;
import com.example.bff.common.exception.KeycloakAdminException;
import com.example.bff.common.exception.UserAlreadyExistsException;
import com.example.bff.common.security.JwtTokenParser;
import com.example.bff.infrastructure.keycloak.KeycloakAuthClient;
import com.example.bff.user.entity.UserEntity;
import com.example.bff.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KeycloakAuthClient keycloakAuthClient;
    private final UserRepository userRepository;
    private final SessionService sessionService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.debug("Registering user: {}", request.username());
        if (userRepository.findByEmail(request.email()).isPresent()
                || userRepository.findByUsername(request.username()).isPresent()) {
            log.warn("User registration failed: user already exists in local DB - {}", request.username());
            throw new UserAlreadyExistsException("User already exists");
        }

        String adminToken = keycloakAuthClient.obtainAdminAccessToken().block();
        if (adminToken == null) {
            log.error("Failed to obtain Keycloak admin token for registration");
            throw new KeycloakAdminException("Unable to obtain Keycloak admin token");
        }

        Boolean existsInKeycloak = keycloakAuthClient
                .userExistsByUsernameOrEmail(request.username(), request.email(), adminToken)
                .blockOptional()
                .orElse(false);

        if (existsInKeycloak) { // Simplified 'Boolean.TRUE.equals(existsInKeycloak)'
            log.warn("User registration failed: user already exists in Keycloak - {}", request.username());
            throw new UserAlreadyExistsException("User already exists");
        }

        String keycloakUserId = keycloakAuthClient.createUser(
                request.username(),
                request.email(),
                request.firstName(),
                request.lastName(),
                request.password(),
                adminToken
        ).block();

        UserEntity user = new UserEntity();
        user.setKeycloakId(keycloakUserId);
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRoles(new HashSet<>(Set.of("ROLE_USER")));
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        log.info("User registered successfully: {} (Keycloak ID: {})", request.username(), keycloakUserId);
        return new RegisterResponse("User registered successfully");
    }

    @Transactional
    public LoginResponse login(LoginRequest request, UUID sessionIdHolder) {
        log.debug("Attempting login for user: {}", request.usernameOrEmail());
        Mono<Map<String, Object>> tokenResponseMono = keycloakAuthClient.login(request.usernameOrEmail(), request.password());
        Map<String, Object> tokenResponse = tokenResponseMono.block();
        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            log.warn("Login failed for user: {} - invalid credentials", request.usernameOrEmail());
            throw new AuthenticationFailedException("Invalid credentials");
        }

        String accessToken = (String) tokenResponse.get("access_token");
        Map<String, Object> claims = JwtTokenParser.parse(accessToken);
        String keycloakUserId = (String) claims.get("sub");
        String username = (String) claims.getOrDefault("preferred_username", request.usernameOrEmail());
        String email = (String) claims.getOrDefault("email", request.usernameOrEmail());
        Set<String> realmRoles = JwtTokenParser.extractRealmRoles(claims);

        Optional<UserEntity> existingByKeycloak = userRepository.findByKeycloakId(keycloakUserId);
        Optional<UserEntity> existingByEmail = userRepository.findByEmail(email);
        Optional<UserEntity> existingByUsername = userRepository.findByUsername(username);

        UserEntity user = existingByKeycloak
                .or(() -> existingByEmail)
                .or(() -> existingByUsername)
                .orElseGet(() -> {
                    log.info("User not found in local DB, synchronizing from Keycloak claims: {}", username);
                    UserEntity created = new UserEntity();
                    created.setKeycloakId(keycloakUserId);
                    created.setUsername(username);
                    created.setEmail(email);
                    created.setFirstName((String) claims.getOrDefault("given_name", ""));
                    created.setLastName((String) claims.getOrDefault("family_name", ""));
                    created.setRoles(realmRoles.isEmpty() ? new HashSet<>(Set.of("ROLE_USER")) : new HashSet<>(realmRoles));
                    created.setCreatedAt(Instant.now());
                    created.setUpdatedAt(Instant.now());
                    return userRepository.save(created);
                });

        // Initialize roles collection to avoid LazyInitializationException during serialization to Redis
        Set<String> sessionRoles = new HashSet<>(realmRoles.isEmpty() ? user.getRoles() : realmRoles);

        Session session = new Session();
        session.setSessionId(sessionIdHolder);
        session.setUserId(user.getId());
        session.setKeycloakUserId(keycloakUserId);
        session.setRoles(sessionRoles);
        session.setAccessToken(accessToken);
        session.setRefreshToken((String) tokenResponse.get("refresh_token"));
        session.setRememberMe(request.rememberMe());
        Instant now = Instant.now();
        Integer expiresIn = (Integer) tokenResponse.getOrDefault("expires_in", 900);
        Integer refreshExpiresIn = (Integer) tokenResponse.getOrDefault("refresh_expires_in", 1800);
        session.setIssuedAt(now);
        session.setAccessTokenExpiresAt(now.plusSeconds(expiresIn));
        session.setRefreshTokenExpiresAt(now.plusSeconds(refreshExpiresIn));
        sessionService.save(session);

        log.info("User logged in and session created: {} (Session ID: {})", user.getUsername(), sessionIdHolder);
        return new LoginResponse(
                "User logged in successfully", // Added message argument
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                sessionRoles
        );
    }

    @Transactional(readOnly = true)
    public MeResponse me(UserEntity user) {
        // Accessing user.getRoles() within a transactional method ensures the collection is initialized
        return new MeResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                new HashSet<>(user.getRoles())
        );
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        log.debug("Forgot password requested for email: {}", request.email());
        String adminToken = keycloakAuthClient.obtainAdminAccessToken().block();
        if (adminToken == null) {
            throw new KeycloakAdminException("Unable to obtain Keycloak admin token");
        }
        String userId = keycloakAuthClient.findUserIdByEmail(request.email(), adminToken).block();
        if (userId == null) {
            log.info("Forgot password: user not found for email {}", request.email());
            return;
        }
        keycloakAuthClient.triggerResetPasswordEmail(userId, adminToken).block();
        log.info("Forgot password reset link triggered for user ID: {}", userId);
    }

    public void logout(String refreshToken) {
        log.debug("Logging out user from Keycloak");
        keycloakAuthClient.logout(refreshToken).subscribe();
    }
}
