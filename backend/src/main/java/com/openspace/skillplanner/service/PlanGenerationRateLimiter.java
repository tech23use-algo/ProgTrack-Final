package com.openspace.skillplanner.service;

import com.openspace.skillplanner.exception.ApiExceptions.GenerationRateLimitException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PlanGenerationRateLimiter {
    private static final int MAX_GENERATIONS = 5;
    private static final Duration WINDOW = Duration.ofHours(1);
    private final Clock clock;
    private final Map<UUID, ArrayDeque<Instant>> requestsByUser = new HashMap<>();

    public PlanGenerationRateLimiter() {
        this(Clock.systemUTC());
    }

    PlanGenerationRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public synchronized void checkAndRecord(UUID userId) {
        Instant now = clock.instant();
        Instant cutoff = now.minus(WINDOW);
        requestsByUser.values().removeIf(requests -> {
            while (!requests.isEmpty() && !requests.peekFirst().isAfter(cutoff)) {
                requests.removeFirst();
            }
            return requests.isEmpty();
        });

        ArrayDeque<Instant> requests = requestsByUser.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        if (requests.size() >= MAX_GENERATIONS) {
            long retryAfter = Math.max(1, Duration.between(now, requests.peekFirst().plus(WINDOW)).toSeconds());
            throw new GenerationRateLimitException(retryAfter);
        }
        requests.addLast(now);
    }
}
