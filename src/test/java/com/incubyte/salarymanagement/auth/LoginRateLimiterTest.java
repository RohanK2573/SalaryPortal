package com.incubyte.salarymanagement.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {
    @Test
    void appliesTenAttemptsPerWindowPerClient() {
        var limiter = new LoginRateLimiter(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                10, Duration.ofMinutes(5));

        for (int attempt = 1; attempt <= 10; attempt++) {
            assertThat(limiter.allow("192.0.2.10")).isTrue();
        }
        assertThat(limiter.allow("192.0.2.10")).isFalse();
        assertThat(limiter.allow("192.0.2.11")).isTrue();
    }
}
