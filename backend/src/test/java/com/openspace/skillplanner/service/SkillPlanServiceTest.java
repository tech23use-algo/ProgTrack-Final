package com.openspace.skillplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.openspace.skillplanner.api.dto.CreateSkillPlanRequest;
import com.openspace.skillplanner.api.dto.SkillPlanResponse;
import com.openspace.skillplanner.domain.SkillPlan;
import com.openspace.skillplanner.exception.ApiExceptions.PlanGenerationFailedException;
import com.openspace.skillplanner.exception.ApiExceptions.SkillNotAcceptedException;
import com.openspace.skillplanner.groq.GroqClient;
import com.openspace.skillplanner.groq.GroqPlan;
import com.openspace.skillplanner.repository.SkillPlanRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SkillPlanServiceTest {
    @Mock
    private SkillPlanRepository repository;
    @Mock
    private GroqClient groqClient;
    @Mock
    private PlanGenerationRateLimiter rateLimiter;

    private SkillPlanService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new SkillPlanService(repository, groqClient, rateLimiter);
        userId = UUID.randomUUID();
    }

    @Test
    void createsAndPersistsAValidatedPlan() {
        CreateSkillPlanRequest request = request("Java", 1, "1.0");
        when(groqClient.generate("Java", 1, new BigDecimal("1.0"), null))
                .thenReturn(validPlan(60));
        when(repository.saveAndFlush(any(SkillPlan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SkillPlanResponse response = service.create(request);

        ArgumentCaptor<SkillPlan> planCaptor = ArgumentCaptor.forClass(SkillPlan.class);
        verify(repository).saveAndFlush(planCaptor.capture());
        verify(rateLimiter).checkAndRecord(userId);
        assertThat(planCaptor.getValue().getPlanDays()).hasSize(1);
        assertThat(planCaptor.getValue().getPlanDays().getFirst().getTasks()).hasSize(1);
        assertThat(response.progress().totalTasks()).isEqualTo(1);
        assertThat(response.progress().totalMinutes()).isEqualTo(60);
        assertThat(response.progress().taskProgressPercent()).isEqualByComparingTo("0.00");
    }

    @Test
    void retriesOnceWhenTheDayDoesNotMeetTheTimeBudget() {
        CreateSkillPlanRequest request = request("Java", 1, "1.0");
        when(groqClient.generate(eq("Java"), eq(1), any(BigDecimal.class), eq(null)))
                .thenReturn(validPlan(20));
        when(groqClient.generate(eq("Java"), eq(1), any(BigDecimal.class), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(validPlan(60));
        when(repository.saveAndFlush(any(SkillPlan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request);

        verify(groqClient).generate(eq("Java"), eq(1), any(BigDecimal.class), org.mockito.ArgumentMatchers.anyString());
        verify(repository).saveAndFlush(any(SkillPlan.class));
    }

    @Test
    void returnsSafeGenerationErrorAfterTwoInvalidPlans() {
        CreateSkillPlanRequest request = request("Java", 1, "1.0");
        when(groqClient.generate(eq("Java"), eq(1), any(BigDecimal.class), any()))
                .thenReturn(validPlan(20));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(PlanGenerationFailedException.class);
        verify(repository, org.mockito.Mockito.never()).saveAndFlush(any(SkillPlan.class));
    }

    @Test
    void rejectsNonSkillRequestsWithoutSavingAPlan() {
        CreateSkillPlanRequest request = request("What is the weather?", 1, "1.0");
        GroqPlan rejected = new GroqPlan("REJECTED", "Please submit a learnable skill.", List.of());
        when(groqClient.generate(eq(request.skill()), eq(1), any(BigDecimal.class), eq(null)))
                .thenReturn(rejected);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(SkillNotAcceptedException.class)
                .hasMessage("Please submit a learnable skill.");
        verify(repository, org.mockito.Mockito.never()).saveAndFlush(any(SkillPlan.class));
    }

    private CreateSkillPlanRequest request(String skill, int days, String hoursPerDay) {
        return new CreateSkillPlanRequest(userId, skill, days, new BigDecimal(hoursPerDay));
    }

    private GroqPlan validPlan(int taskMinutes) {
        return new GroqPlan(
                "OK",
                "",
                List.of(new GroqPlan.GroqPlanDay(
                        1,
                        "Fundamentals",
                        "Understand the core concepts.",
                        List.of(new GroqPlan.GroqPlanTask(
                                "Complete a focused exercise",
                                "Build and verify a small working example.",
                                taskMinutes)))));
    }
}
