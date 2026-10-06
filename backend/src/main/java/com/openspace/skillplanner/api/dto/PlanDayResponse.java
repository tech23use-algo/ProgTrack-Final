package com.openspace.skillplanner.api.dto;

import java.util.List;

public record PlanDayResponse(
        int dayNumber,
        String title,
        String goal,
        List<PlanTaskResponse> tasks) {
}
