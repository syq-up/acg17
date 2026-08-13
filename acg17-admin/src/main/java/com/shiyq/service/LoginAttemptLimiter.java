package com.shiyq.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单实例部署使用的轻量登录失败限制器。
 */
@Component
public class LoginAttemptLimiter {

    private static final int MAX_TRACKED_KEYS = 10000;

    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long windowMillis;

    public LoginAttemptLimiter(@Value("${security.login.max-attempts:10}") int maxAttempts,
                               @Value("${security.login.window-minutes:5}") long windowMinutes) {
        if (maxAttempts <= 0 || windowMinutes <= 0) {
            throw new IllegalArgumentException("Login rate limit settings must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowMinutes * 60_000L;
    }

    public boolean isBlocked(String key) {
        long now = System.currentTimeMillis();
        AttemptWindow window = attempts.get(key);
        if (window == null) {
            if (attempts.size() >= MAX_TRACKED_KEYS) {
                attempts.forEach((attemptKey, attemptWindow) -> {
                    if (attemptWindow.expiresAt <= now) {
                        attempts.remove(attemptKey, attemptWindow);
                    }
                });
            }
            return attempts.size() >= MAX_TRACKED_KEYS;
        }
        if (window.expiresAt <= now) {
            attempts.remove(key, window);
            return false;
        }
        return window.failures >= maxAttempts;
    }

    public void recordFailure(String key) {
        long now = System.currentTimeMillis();
        attempts.compute(key, (ignored, current) -> {
            if (current == null || current.expiresAt <= now) {
                return new AttemptWindow(1, now + windowMillis);
            }
            current.failures++;
            return current;
        });
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private static final class AttemptWindow {
        private int failures;
        private final long expiresAt;

        private AttemptWindow(int failures, long expiresAt) {
            this.failures = failures;
            this.expiresAt = expiresAt;
        }
    }
}
