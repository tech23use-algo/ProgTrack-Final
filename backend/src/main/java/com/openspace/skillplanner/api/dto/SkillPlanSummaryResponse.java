package com.openspace.skillplanner.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SkillPlanSummaryResponse(
        UUID id,
        UUID userId,
        String skill,
        int days,
        BigDecimal hoursPerDay,
        Instant createdAt,
        ProgressResponse progress) {
}
