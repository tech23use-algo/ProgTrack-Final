package com.openspace.skillplanner.repository;

import com.openspace.skillplanner.domain.SkillPlan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillPlanRepository extends JpaRepository<SkillPlan, UUID> {
    @EntityGraph(attributePaths = {"planDays", "planDays.tasks"})
    Optional<SkillPlan> findWithDaysAndTasksById(UUID id);

    @EntityGraph(attributePaths = {"planDays", "planDays.tasks"})
    List<SkillPlan> findWithDaysAndTasksByUserIdOrderByCreatedAtDesc(UUID userId);
}
