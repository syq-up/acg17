package com.shiyq.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptLimiterTest {

    @Test
    void shouldBlockAfterConfiguredFailuresAndResetAfterSuccess() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(2, 5);
        String key = "127.0.0.1:user";

        assertFalse(limiter.isBlocked(key));
        limiter.recordFailure(key);
        assertFalse(limiter.isBlocked(key));
        limiter.recordFailure(key);
        assertTrue(limiter.isBlocked(key));

        limiter.reset(key);
        assertFalse(limiter.isBlocked(key));
    }
}
