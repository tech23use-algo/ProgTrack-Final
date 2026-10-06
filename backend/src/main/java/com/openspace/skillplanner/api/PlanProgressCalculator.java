package com.openspace.skillplanner.api;

import com.openspace.skillplanner.api.dto.ProgressResponse;
import com.openspace.skillplanner.domain.PlanDay;
import com.openspace.skillplanner.domain.PlanTask;
import com.openspace.skillplanner.domain.SkillPlan;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PlanProgressCalculator {
    private PlanProgressCalculator() {
    }

    public static ProgressResponse calculate(SkillPlan plan) {
        int totalTasks = 0;
        int completedTasks = 0;
        int totalMinutes = 0;
        int completedMinutes = 0;

        for (PlanDay day : plan.getPlanDays()) {
            for (PlanTask task : day.getTasks()) {
                totalTasks++;
                totalMinutes += task.getEstimatedMinutes();
                if (task.isCompleted()) {
                    completedTasks++;
                    completedMinutes += task.getEstimatedMinutes();
                }
            }
        }

        return new ProgressResponse(
                completedTasks,
                totalTasks,
                completedMinutes,
                totalMinutes,
                percentage(completedTasks, totalTasks),
                percentage(completedMinutes, totalMinutes));
    }

    private static BigDecimal percentage(int completed, int total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
