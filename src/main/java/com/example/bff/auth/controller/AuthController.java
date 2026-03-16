package com.example.bff.auth.controller;

import com.example.bff.auth.dto.ForgotPasswordRequest;
import com.example.bff.auth.dto.LoginRequest;
import com.example.bff.auth.dto.LoginResponse;
import com.example.bff.auth.dto.LogoutResponse;
import com.example.bff.auth.dto.MeResponse;
import com.example.bff.auth.dto.RegisterRequest;
import com.example.bff.auth.dto.RegisterResponse;
import com.example.bff.auth.service.AuthService;
import com.example.bff.auth.service.SessionService;
import com.example.bff.common.api.ApiResponse;
import com.example.bff.config.SecurityProperties;
import com.example.bff.user.entity.UserEntity;
import com.example.bff.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and session management endpoints")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final SecurityProperties securityProperties;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Registers a new user in Keycloak and synchronizes them into the application database.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully",
            content = @Content(schema = @Schema(implementation = RegisterResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "User already exists",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Received registration request for user: {}", request.username());
        RegisterResponse response = authService.register(request);
        ApiResponse<RegisterResponse> body =
                ApiResponse.success(null, HttpStatus.CREATED.value(), response.message(), response);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates the user via Keycloak and establishes a BFF session via HttpOnly cookie.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User logged in successfully",
            content = @Content(schema = @Schema(implementation = LoginResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletResponse servletResponse) {
        log.info("Received login request for user: {}", request.usernameOrEmail());
        UUID sessionId = UUID.randomUUID();
        LoginResponse response = authService.login(request, sessionId);

        Cookie cookie = new Cookie(securityProperties.getSessionCookieName(), sessionId.toString());
        cookie.setHttpOnly(true);
        cookie.setSecure(securityProperties.isSessionCookieSecure());
        cookie.setPath("/");
        cookie.setAttribute("SameSite", securityProperties.getSessionCookieSameSite());
        
        if (request.rememberMe()) {
            // 30 days
            cookie.setMaxAge(60 * 60 * 24 * 30);
        } else {
            // session cookie
            cookie.setMaxAge(-1);
        }
        
        servletResponse.addCookie(cookie);
        log.debug("Set-Cookie header added for session: {}", sessionId);

        ApiResponse<LoginResponse> body =
                ApiResponse.success(null, HttpStatus.OK.value(), response.message(), response);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns the non-sensitive profile of the currently authenticated user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User profile returned",
            content = @Content(schema = @Schema(implementation = MeResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<MeResponse>> me(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        log.debug("Fetching profile for user ID: {}", userId);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.failure(null, HttpStatus.UNAUTHORIZED.value(), "Not authenticated", null));
        }
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User profile not found for user ID: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure(null, HttpStatus.NOT_FOUND.value(), "User not found", null));
        }
        MeResponse response = authService.me(userOpt.get());
        ApiResponse<MeResponse> body =
                ApiResponse.success(null, HttpStatus.OK.value(), "User profile fetched successfully", response);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Logs out the current user by clearing the BFF session and session cookie.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User logged out successfully",
            content = @Content(schema = @Schema(implementation = LogoutResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No active session",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (securityProperties.getSessionCookieName().equals(cookie.getName())) {
                    try {
                        UUID sessionId = UUID.fromString(cookie.getValue());
                        log.info("Logging out session: {}", sessionId);
                        sessionService.findById(sessionId).ifPresent(session -> {
                            authService.logout(session.getRefreshToken());
                            sessionService.delete(sessionId);
                        });
                    } catch (IllegalArgumentException ignored) {
                    }
                    cookie.setValue("");
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                }
            }
        }
        LogoutResponse payload = new LogoutResponse("User logged out successfully");
        ApiResponse<LogoutResponse> body =
                ApiResponse.success(null, HttpStatus.OK.value(), payload.message(), payload);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Trigger forgot password", description = "Triggers a password reset email for the given email address if the user exists.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reset email trigger accepted",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Received forgot-password request for email: {}", request.email());
        authService.forgotPassword(request);
        ApiResponse<Void> body = ApiResponse.success(null, HttpStatus.OK.value(),
                "If the email exists, a reset link has been sent", null);
        return ResponseEntity.ok(body);
    }
}
