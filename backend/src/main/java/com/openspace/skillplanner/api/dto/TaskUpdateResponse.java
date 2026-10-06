package com.openspace.skillplanner.api.dto;

import java.time.Instant;
import java.util.UUID;

public record TaskUpdateResponse(
        UUID taskId,
        boolean completed,
        Instant completedAt,
        ProgressResponse progress) {
}
