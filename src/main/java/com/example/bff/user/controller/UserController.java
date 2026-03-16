package com.example.bff.user.controller;

import com.example.bff.common.api.ApiResponse;
import com.example.bff.user.entity.UserEntity;
import com.example.bff.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User and role-based access endpoints")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user (entity)", description = "Returns the current authenticated user entity for debugging and inspection.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Current user returned",
            content = @Content(schema = @Schema(implementation = UserEntity.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<UserEntity>> me(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        log.debug("User entity requested for ID: {}", userId);
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        return userOpt
                .map(user -> ResponseEntity.ok(ApiResponse.success(null, HttpStatus.OK.value(), "Current user", user)))
                .orElseGet(() -> {
                    log.warn("User not found: {}", userId);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.failure(null, HttpStatus.NOT_FOUND.value(), "User not found", null));
                });
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users (admin)", description = "Admin-only endpoint to list all users.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All users returned",
            content = @Content(schema = @Schema(implementation = UserEntity.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<List<UserEntity>>> all() {
        log.info("Admin listing all users");
        List<UserEntity> users = userRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(null, HttpStatus.OK.value(), "All users", users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Get user by id", description = "Returns user details for the given id. ADMIN can access any user; USER can only access themselves (enforced at app level if desired).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User returned",
            content = @Content(schema = @Schema(implementation = UserEntity.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<UserEntity>> byId(@PathVariable UUID id) {
        log.debug("Fetching user by ID: {}", id);
        Optional<UserEntity> userOpt = userRepository.findById(id);
        return userOpt
                .map(user -> ResponseEntity.ok(ApiResponse.success(null, HttpStatus.OK.value(), "User", user)))
                .orElseGet(() -> {
                    log.warn("User not found by ID: {}", id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.failure(null, HttpStatus.NOT_FOUND.value(), "User not found", null));
                });
    }
}
