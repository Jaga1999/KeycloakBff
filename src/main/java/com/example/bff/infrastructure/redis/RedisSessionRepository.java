package com.example.bff.infrastructure.redis;

import com.example.bff.auth.model.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisSessionRepository {

    private static final String KEY_PREFIX = "session:";

    private final RedisTemplate<String, Session> redisTemplate;

    public void save(Session session, Duration ttl) {
        String key = KEY_PREFIX + session.getSessionId();
        log.debug("Saving session to Redis: {} with TTL: {}", key, ttl);
        redisTemplate.opsForValue().set(key, session, ttl);
    }

    public Optional<Session> findById(UUID sessionId) {
        String key = KEY_PREFIX + sessionId;
        log.trace("Fetching session from Redis: {}", key);
        Session session = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(session);
    }

    public void delete(UUID sessionId) {
        String key = KEY_PREFIX + sessionId;
        log.debug("Deleting session from Redis: {}", key);
        redisTemplate.delete(key);
    }
}
