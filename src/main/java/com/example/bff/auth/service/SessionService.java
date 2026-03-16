package com.example.bff.auth.service;

import com.example.bff.auth.model.Session;
import com.example.bff.infrastructure.redis.RedisSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final RedisSessionRepository repository;

    public void save(Session session) {
        Instant now = Instant.now();
        Instant expires = session.getRefreshTokenExpiresAt() != null ? session.getRefreshTokenExpiresAt() : now.plus(Duration.ofHours(1));
        Duration ttl = Duration.between(now, expires);
        log.debug("Saving session {} with TTL: {}s", session.getSessionId(), ttl.getSeconds());
        repository.save(session, ttl);
    }

    public Optional<Session> findById(UUID sessionId) {
        log.trace("Finding session by ID: {}", sessionId);
        return repository.findById(sessionId);
    }

    public void delete(UUID sessionId) {
        log.info("Deleting session: {}", sessionId);
        repository.delete(sessionId);
    }
}
