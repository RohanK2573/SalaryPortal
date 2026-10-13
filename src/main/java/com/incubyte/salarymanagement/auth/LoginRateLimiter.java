package com.incubyte.salarymanagement.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {
    private final ConcurrentHashMap<String, Window> attempts = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int limit;
    private final Duration window;

    public LoginRateLimiter(Clock clock,
            @Value("${app.security.login-attempt-limit:10}") int limit,
            @Value("${app.security.login-attempt-window:5m}") Duration window) {
        this.clock = clock;
        this.limit = limit;
        this.window = window;
    }

    public boolean allow(String clientIp) {
        Instant now = clock.instant();
        Window windowState = attempts.compute(clientIp, (key, previous) -> {
            if (previous == null || !now.isBefore(previous.startedAt().plus(window))) {
                return new Window(now, 1);
            }
            return new Window(previous.startedAt(), previous.count() + 1);
        });
        if (attempts.size() > 10_000) {
            attempts.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().startedAt().plus(window)));
        }
        return windowState.count() <= limit;
    }

    private record Window(Instant startedAt, int count) {}
}
