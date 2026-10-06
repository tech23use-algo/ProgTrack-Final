package com.openspace.skillplanner.api;

import com.openspace.skillplanner.api.dto.CreateSkillPlanRequest;
import com.openspace.skillplanner.api.dto.SkillPlanResponse;
import com.openspace.skillplanner.api.dto.SkillPlanSummaryResponse;
import com.openspace.skillplanner.api.dto.TaskUpdateResponse;
import com.openspace.skillplanner.api.dto.UpdateTaskRequest;
import com.openspace.skillplanner.service.SkillPlanService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api")
public class SkillPlanController {
    private final SkillPlanService service;

    public SkillPlanController(SkillPlanService service) {
        this.service = service;
    }

    @PostMapping("/skill-plans")
    public ResponseEntity<SkillPlanResponse> create(@Valid @RequestBody CreateSkillPlanRequest request) {
        SkillPlanResponse plan = service.create(request);
        return ResponseEntity.created(URI.create("/api/skill-plans/" + plan.id())).body(plan);
    }

    @GetMapping("/skill-plans/{planId}")
    public SkillPlanResponse get(@PathVariable UUID planId) {
        return service.get(planId);
    }

    @GetMapping("/users/{userId}/skill-plans")
    public List<SkillPlanSummaryResponse> listForUser(@PathVariable UUID userId) {
        return service.listForUser(userId);
    }

    @PatchMapping("/skill-plans/{planId}/tasks/{taskId}")
    public TaskUpdateResponse updateTask(
            @PathVariable UUID planId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        return service.updateTask(planId, taskId, request.completed());
    }

    @DeleteMapping("/skill-plans/{planId}")
    public ResponseEntity<Void> delete(@PathVariable UUID planId) {
        service.delete(planId);
        return ResponseEntity.noContent().build();
    }
}
