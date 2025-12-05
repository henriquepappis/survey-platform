package com.survey.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBlacklist {

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();
    private final Clock clock;

    public TokenBlacklist() {
        this.clock = Clock.systemUTC();
    }

    public boolean isBlacklisted(String token) {
        Long expiresAt = blacklist.get(token);
        if (expiresAt == null) {
            return false;
        }

        long now = clock.millis();
        if (expiresAt <= now) {
            blacklist.remove(token);
            return false;
        }

        return true;
    }

    public void blacklist(String token, Instant expiresAt) {
        blacklist.put(token, expiresAt.toEpochMilli());
        cleanupExpired();
    }

    private void cleanupExpired() {
        long now = clock.millis();
        blacklist.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
