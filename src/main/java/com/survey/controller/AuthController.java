package com.survey.controller;

import com.survey.dto.AuthRequest;
import com.survey.dto.AuthResponse;
import com.survey.security.JwtTokenProvider;
import com.survey.security.TokenBlacklist;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklist tokenBlacklist;
    private final long expirationMs;
    private final int maxAttempts;
    private final long windowMs;
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider tokenProvider,
                          TokenBlacklist tokenBlacklist,
                          @Value("${app.security.jwt.expiration:3600000}") long expirationMs,
                          @Value("${app.security.login.max-attempts:5}") int maxAttempts,
                          @Value("${app.security.login.window-ms:60000}") long windowMs) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.tokenBlacklist = tokenBlacklist;
        this.expirationMs = expirationMs;
        this.maxAttempts = maxAttempts;
        this.windowMs = windowMs;
        this.clock = Clock.systemUTC();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest != null ? httpRequest.getRemoteAddr() : "unknown";
        String key = clientIp + "|" + request.getUsername();

        if (isBlocked(key)) {
            return ResponseEntity.status(429).build();
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            String token = tokenProvider.generateToken(authentication);
            Instant expiresAt = Instant.now().plusMillis(expirationMs);
            resetAttempts(key);
            return ResponseEntity.ok(new AuthResponse(token, expiresAt));
        } catch (AuthenticationException ex) {
            registerFailure(key);
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null || !tokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }

        tokenBlacklist.blacklist(token, tokenProvider.getExpiration(token));
        return ResponseEntity.noContent().build();
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request != null ? request.getHeader("Authorization") : null;
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    private boolean isBlocked(String key) {
        long now = clock.millis();
        Attempt attempt = attempts.computeIfAbsent(key, k -> new Attempt(now, 0));
        synchronized (attempt) {
            if (now - attempt.windowStart > windowMs) {
                attempt.windowStart = now;
                attempt.count = 0;
            }
            return attempt.count >= maxAttempts;
        }
    }

    private void registerFailure(String key) {
        long now = clock.millis();
        Attempt attempt = attempts.computeIfAbsent(key, k -> new Attempt(now, 0));
        synchronized (attempt) {
            if (now - attempt.windowStart > windowMs) {
                attempt.windowStart = now;
                attempt.count = 0;
            }
            attempt.count++;
        }
    }

    private void resetAttempts(String key) {
        attempts.remove(key);
    }

    private static class Attempt {
        long windowStart;
        int count;

        Attempt(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
