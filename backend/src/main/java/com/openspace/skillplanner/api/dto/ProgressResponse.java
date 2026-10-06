package com.openspace.skillplanner.api.dto;

import java.math.BigDecimal;

public record ProgressResponse(
        int completedTasks,
        int totalTasks,
        int completedMinutes,
        int totalMinutes,
        BigDecimal taskProgressPercent,
        BigDecimal timeProgressPercent) {
}
