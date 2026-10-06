package com.openspace.skillplanner.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SkillPlanResponse(
        UUID id,
        UUID userId,
        String skill,
        int days,
        BigDecimal hoursPerDay,
        Instant createdAt,
        List<PlanDayResponse> planDays,
        ProgressResponse progress) {
}
