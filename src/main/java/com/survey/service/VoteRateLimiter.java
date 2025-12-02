package com.survey.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter simples por IP para endpoints públicos de voto.
 */
@Component
public class VoteRateLimiter {

    private final int maxPerMinute;
    private final Clock clock;
    private final Map<String, Window> buckets = new ConcurrentHashMap<>();

    public VoteRateLimiter(@Value("${app.votes.rate-limit.max-per-minute:30}") int maxPerMinute,
                           Clock clock) {
        this.maxPerMinute = maxPerMinute;
        this.clock = clock;
    }

    public boolean allow(String ip) {
        if (ip == null || ip.isBlank()) {
            ip = "unknown";
        }
        long now = clock.millis();
        Window window = buckets.computeIfAbsent(ip, k -> new Window(now, 0));
        synchronized (window) {
            if (now - window.windowStart >= 60_000) {
                window.windowStart = now;
                window.count = 0;
            }
            if (window.count >= maxPerMinute) {
                return false;
            }
            window.count++;
            return true;
        }
    }

    private static class Window {
        long windowStart;
        int count;

        Window(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
