package com.openspace.skillplanner.service;

import com.openspace.skillplanner.api.PlanProgressCalculator;
import com.openspace.skillplanner.api.dto.CreateSkillPlanRequest;
import com.openspace.skillplanner.api.dto.PlanDayResponse;
import com.openspace.skillplanner.api.dto.PlanTaskResponse;
import com.openspace.skillplanner.api.dto.ProgressResponse;
import com.openspace.skillplanner.api.dto.SkillPlanResponse;
import com.openspace.skillplanner.api.dto.SkillPlanSummaryResponse;
import com.openspace.skillplanner.api.dto.TaskUpdateResponse;
import com.openspace.skillplanner.domain.PlanDay;
import com.openspace.skillplanner.domain.PlanTask;
import com.openspace.skillplanner.domain.SkillPlan;
import com.openspace.skillplanner.exception.ApiExceptions.PlanGenerationFailedException;
import com.openspace.skillplanner.exception.ApiExceptions.PlanNotFoundException;
import com.openspace.skillplanner.exception.ApiExceptions.SkillNotAcceptedException;
import com.openspace.skillplanner.exception.ApiExceptions.TaskNotFoundException;
import com.openspace.skillplanner.exception.ApiExceptions.UpstreamAuthenticationException;
import com.openspace.skillplanner.exception.ApiExceptions.UpstreamTimeoutException;
import com.openspace.skillplanner.exception.ApiExceptions.UpstreamUnavailableException;
import com.openspace.skillplanner.groq.GroqClient;
import com.openspace.skillplanner.groq.GroqClient.InvalidGroqResponseException;
import com.openspace.skillplanner.groq.GroqPlan;
import com.openspace.skillplanner.repository.SkillPlanRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkillPlanService {
    private static final Logger logger = LoggerFactory.getLogger(SkillPlanService.class);
    private static final int MAX_GENERATION_ATTEMPTS = 2;

    private final SkillPlanRepository repository;
    private final GroqClient groqClient;
    private final PlanGenerationRateLimiter rateLimiter;
    private final Clock clock;

    @Autowired
    public SkillPlanService(
            SkillPlanRepository repository,
            GroqClient groqClient,
            PlanGenerationRateLimiter rateLimiter) {
        this(repository, groqClient, rateLimiter, Clock.systemUTC());
    }

    SkillPlanService(
            SkillPlanRepository repository,
            GroqClient groqClient,
            PlanGenerationRateLimiter rateLimiter,
            Clock clock) {
        this.repository = repository;
        this.groqClient = groqClient;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    public SkillPlanResponse create(CreateSkillPlanRequest request) {
        rateLimiter.checkAndRecord(request.userId());

        GroqPlan generated = generateAndValidate(request);
        SkillPlan plan = new SkillPlan(
                request.userId(),
                request.skill().trim(),
                request.days(),
                request.hoursPerDay(),
                clock.instant());
        for (GroqPlan.GroqPlanDay generatedDay : generated.days()) {
            PlanDay planDay = new PlanDay(
                    generatedDay.dayNumber(),
                    generatedDay.title().trim(),
                    generatedDay.goal().trim());
            for (GroqPlan.GroqPlanTask generatedTask : generatedDay.tasks()) {
                planDay.addTask(new PlanTask(
                        generatedTask.title().trim(),
                        generatedTask.description().trim(),
                        generatedTask.estimatedMinutes()));
            }
            plan.addDay(planDay);
        }

        SkillPlan savedPlan = repository.saveAndFlush(plan);
        logger.info("skill_plan_created userId={} planId={} days={}",
                savedPlan.getUserId(), savedPlan.getId(), savedPlan.getDays());
        return toResponse(savedPlan);
    }

    @Transactional(readOnly = true)
    public SkillPlanResponse get(UUID planId) {
        return toResponse(findPlan(planId));
    }

    @Transactional(readOnly = true)
    public List<SkillPlanSummaryResponse> listForUser(UUID userId) {
        return repository.findWithDaysAndTasksByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public TaskUpdateResponse updateTask(UUID planId, UUID taskId, boolean completed) {
        SkillPlan plan = findPlan(planId);
        PlanTask matchingTask = plan.getPlanDays().stream()
                .flatMap(day -> day.getTasks().stream())
                .filter(task -> task.getId().equals(taskId))
                .findFirst()
                .orElseThrow(TaskNotFoundException::new);

        Instant completedAt = completed
                ? (matchingTask.isCompleted() ? matchingTask.getCompletedAt() : clock.instant())
                : null;
        matchingTask.setCompleted(completed, completedAt);
        return new TaskUpdateResponse(
                matchingTask.getId(),
                matchingTask.isCompleted(),
                matchingTask.getCompletedAt(),
                PlanProgressCalculator.calculate(plan));
    }

    @Transactional
    public void delete(UUID planId) {
        SkillPlan plan = findPlan(planId);
        repository.delete(plan);
        logger.info("skill_plan_deleted planId={}", planId);
    }

    private GroqPlan generateAndValidate(CreateSkillPlanRequest request) {
        String validationError = null;
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            try {
                GroqPlan response = groqClient.generate(
                        request.skill(),
                        request.days(),
                        request.hoursPerDay(),
                        validationError);
                validateResponse(response, request);
                return response;
            } catch (InvalidGroqResponseException | InvalidPlanContentException exception) {
                validationError = exception.getMessage();
                logger.warn("groq_plan_validation_failed attempt={} reason={}", attempt + 1, validationError);
            } catch (UpstreamAuthenticationException | UpstreamUnavailableException
                     | UpstreamTimeoutException | PlanGenerationFailedException exception) {
                throw exception;
            } catch (SkillNotAcceptedException exception) {
                throw exception;
            }
        }
        throw new PlanGenerationFailedException();
    }

    private void validateResponse(GroqPlan response, CreateSkillPlanRequest request) {
        if (response == null || response.status() == null || response.reason() == null || response.days() == null) {
            throw new InvalidPlanContentException("The response was missing required fields.");
        }
        if ("REJECTED".equals(response.status())) {
            if (response.reason().isBlank() || !response.days().isEmpty()) {
                throw new InvalidPlanContentException("The rejected response did not match the required schema.");
            }
            throw new SkillNotAcceptedException(limitReason(response.reason()));
        }
        if (!"OK".equals(response.status())) {
            throw new InvalidPlanContentException("The response status was not recognized.");
        }
        if (!response.reason().isEmpty()) {
            throw new InvalidPlanContentException("A successful response must have an empty reason.");
        }
        if (response.days().size() != request.days()) {
            throw new InvalidPlanContentException(
                    "Expected exactly " + request.days() + " day entries but received " + response.days().size() + ".");
        }
        if (response.days().stream().anyMatch(Objects::isNull)) {
            throw new InvalidPlanContentException("Day entries must not be null.");
        }

        List<GroqPlan.GroqPlanDay> orderedDays = response.days().stream()
                .sorted(Comparator.comparingInt(GroqPlan.GroqPlanDay::dayNumber))
                .toList();
        BigDecimal expectedMinutes = request.hoursPerDay().multiply(BigDecimal.valueOf(60));
        BigDecimal minimum = expectedMinutes.multiply(new BigDecimal("0.90"));
        BigDecimal maximum = expectedMinutes.multiply(new BigDecimal("1.10"));

        for (int index = 0; index < orderedDays.size(); index++) {
            GroqPlan.GroqPlanDay day = orderedDays.get(index);
            if (day == null || day.dayNumber() != index + 1
                    || isBlank(day.title()) || day.title().length() > 200
                    || isBlank(day.goal()) || day.tasks() == null || day.tasks().isEmpty()) {
                throw new InvalidPlanContentException("Day entries must have sequential numbers, titles, goals, and tasks.");
            }

            long dayMinutes = 0;
            for (GroqPlan.GroqPlanTask task : day.tasks()) {
                if (task == null || isBlank(task.title()) || task.title().length() > 240
                        || isBlank(task.description()) || task.estimatedMinutes() < 1) {
                    throw new InvalidPlanContentException("Every task must have a title, description, and positive duration.");
                }
                dayMinutes += task.estimatedMinutes();
            }
            BigDecimal actual = BigDecimal.valueOf(dayMinutes);
            if (actual.compareTo(minimum) < 0 || actual.compareTo(maximum) > 0) {
                throw new InvalidPlanContentException(
                        "Day " + (index + 1) + " task minutes must be within 10% of "
                                + expectedMinutes.stripTrailingZeros().toPlainString() + ".");
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String limitReason(String reason) {
        String oneLine = reason.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (oneLine.length() <= 240) {
            return oneLine;
        }
        return oneLine.substring(0, 237) + "...";
    }

    private SkillPlan findPlan(UUID planId) {
        return repository.findWithDaysAndTasksById(planId).orElseThrow(PlanNotFoundException::new);
    }

    private SkillPlanResponse toResponse(SkillPlan plan) {
        List<PlanDayResponse> days = plan.getPlanDays().stream()
                .sorted(Comparator.comparingInt(PlanDay::getDayNumber))
                .map(day -> new PlanDayResponse(
                        day.getDayNumber(),
                        day.getTitle(),
                        day.getGoal(),
                        day.getTasks().stream()
                                .map(this::toTaskResponse)
                                .toList()))
                .toList();
        return new SkillPlanResponse(
                plan.getId(),
                plan.getUserId(),
                plan.getSkill(),
                plan.getDays(),
                plan.getHoursPerDay(),
                plan.getCreatedAt(),
                days,
                PlanProgressCalculator.calculate(plan));
    }

    private SkillPlanSummaryResponse toSummary(SkillPlan plan) {
        return new SkillPlanSummaryResponse(
                plan.getId(),
                plan.getUserId(),
                plan.getSkill(),
                plan.getDays(),
                plan.getHoursPerDay(),
                plan.getCreatedAt(),
                PlanProgressCalculator.calculate(plan));
    }

    private PlanTaskResponse toTaskResponse(PlanTask task) {
        return new PlanTaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getEstimatedMinutes(),
                task.isCompleted(),
                task.getCompletedAt());
    }

    private static final class InvalidPlanContentException extends RuntimeException {
        private InvalidPlanContentException(String message) {
            super(message);
        }
    }
}
