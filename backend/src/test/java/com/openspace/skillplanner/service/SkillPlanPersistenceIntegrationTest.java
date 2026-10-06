package com.openspace.skillplanner.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.openspace.skillplanner.api.dto.TaskUpdateResponse;
import com.openspace.skillplanner.domain.PlanDay;
import com.openspace.skillplanner.domain.PlanTask;
import com.openspace.skillplanner.domain.SkillPlan;
import com.openspace.skillplanner.repository.SkillPlanRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SkillPlanPersistenceIntegrationTest {
    @Autowired
    private SkillPlanRepository repository;

    @Autowired
    private SkillPlanService service;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsNestedPlanAndUpdatesTaskAndTimeProgress() {
        SkillPlan plan = new SkillPlan(
                UUID.randomUUID(),
                "Java",
                1,
                new BigDecimal("1.0"),
                Instant.now());
        PlanDay day = new PlanDay(1, "Foundations", "Practice the core syntax.");
        day.addTask(new PlanTask("Write examples", "Write and run small examples.", 45));
        day.addTask(new PlanTask("Review output", "Check the results against expected values.", 15));
        plan.addDay(day);

        SkillPlan savedPlan = repository.saveAndFlush(plan);
        UUID planId = savedPlan.getId();
        UUID taskId = savedPlan.getPlanDays().getFirst().getTasks().getFirst().getId();
        entityManager.clear();

        SkillPlan loadedPlan = repository.findWithDaysAndTasksById(planId).orElseThrow();
        assertThat(loadedPlan.getPlanDays()).hasSize(1);
        assertThat(loadedPlan.getPlanDays().getFirst().getTasks())
                .extracting(PlanTask::getTitle)
                .containsExactly("Write examples", "Review output");

        TaskUpdateResponse updated = service.updateTask(planId, taskId, true);

        assertThat(updated.completed()).isTrue();
        assertThat(updated.completedAt()).isNotNull();
        assertThat(updated.progress().completedTasks()).isEqualTo(1);
        assertThat(updated.progress().totalTasks()).isEqualTo(2);
        assertThat(updated.progress().completedMinutes()).isEqualTo(45);
        assertThat(updated.progress().totalMinutes()).isEqualTo(60);
        assertThat(updated.progress().taskProgressPercent()).isEqualByComparingTo("50.00");
        assertThat(updated.progress().timeProgressPercent()).isEqualByComparingTo("75.00");
    }
}
