package com.openspace.skillplanner.api.dto;

import java.time.Instant;
import java.util.UUID;

public record PlanTaskResponse(
        UUID id,
        String title,
        String description,
        int estimatedMinutes,
        boolean completed,
        Instant completedAt) {
}
