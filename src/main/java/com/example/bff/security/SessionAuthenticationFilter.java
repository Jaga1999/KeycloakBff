package com.example.bff.security;

import com.example.bff.auth.model.Session;
import com.example.bff.auth.service.SessionService;
import com.example.bff.common.exception.TokenRefreshException;
import com.example.bff.infrastructure.keycloak.KeycloakAuthClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final SessionService sessionService;
    private final KeycloakAuthClient keycloakAuthClient;
    private final String cookieName;
    private final List<String> publicPaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public SessionAuthenticationFilter(SessionService sessionService, KeycloakAuthClient keycloakAuthClient, String cookieName, List<String> publicPaths) {
        this.sessionService = sessionService;
        this.keycloakAuthClient = keycloakAuthClient;
        this.cookieName = cookieName;
        this.publicPaths = publicPaths;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        return publicPaths.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String sessionIdValue = resolveSessionIdFromCookie(request);
            if (sessionIdValue != null) {
                try {
                    UUID sessionId = UUID.fromString(sessionIdValue);
                    log.trace("Attempting authentication for session: {}", sessionId);
                    Optional<Session> sessionOpt = sessionService.findById(sessionId);
                    if (sessionOpt.isPresent()) {
                        Session session = sessionOpt.get();

                        if (isTokenExpired(session) && isRefreshTokenValid(session)) {
                            refreshAccessToken(session);
                        }

                        if (isTokenValid(session)) {
                            authenticateUser(session, request);
                        }
                    } else {
                        log.warn("Session not found in store: {}", sessionId);
                    }
                } catch (IllegalArgumentException ignored) {
                    log.warn("Invalid session ID format in cookie: {}", sessionIdValue);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isTokenExpired(Session session) {
        return session.getAccessTokenExpiresAt() != null && session.getAccessTokenExpiresAt().isBefore(Instant.now());
    }

    private boolean isRefreshTokenValid(Session session) {
        return session.getRefreshTokenExpiresAt() != null && session.getRefreshTokenExpiresAt().isAfter(Instant.now());
    }

    private boolean isTokenValid(Session session) {
        return session.getAccessTokenExpiresAt() == null || session.getAccessTokenExpiresAt().isAfter(Instant.now());
    }

    private void refreshAccessToken(Session session) {
        log.debug("Refreshing access token for session: {}", session.getSessionId());
        Map<String, Object> refreshResponse = keycloakAuthClient.refreshToken(session.getRefreshToken()).block();
        if (refreshResponse == null || !refreshResponse.containsKey("access_token")) {
            log.error("Failed to refresh access token for session: {}", session.getSessionId());
            throw new TokenRefreshException("Unable to refresh access token");
        }
        String newAccessToken = (String) refreshResponse.get("access_token");
        Integer expiresIn = (Integer) refreshResponse.getOrDefault("expires_in", 900);
        session.setAccessToken(newAccessToken);
        session.setAccessTokenExpiresAt(Instant.now().plusSeconds(expiresIn));
        sessionService.save(session);
        log.debug("Access token refreshed successfully for session: {}", session.getSessionId());
    }

    private void authenticateUser(Session session, HttpServletRequest request) {
        List<GrantedAuthority> authorities = session.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                session.getUserId(), null, authorities);
        authentication
                .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.trace("User {} authenticated via session {}", session.getUserId(), session.getSessionId());
    }

    private String resolveSessionIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
