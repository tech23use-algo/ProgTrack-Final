package com.openspace.skillplanner.groq;

import java.util.List;

public record GroqPlan(String status, String reason, List<GroqPlanDay> days) {
    public record GroqPlanDay(int dayNumber, String title, String goal, List<GroqPlanTask> tasks) {
    }

    public record GroqPlanTask(String title, String description, int estimatedMinutes) {
    }
}
