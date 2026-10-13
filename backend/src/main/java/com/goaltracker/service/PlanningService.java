package com.goaltracker.service;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.goaltracker.ai.GroqClient;
import com.goaltracker.controller.AiRejectedException;
import com.goaltracker.dto.Requests;
import com.goaltracker.dto.Responses;
import com.goaltracker.model.Goal;
import com.goaltracker.model.Milestone;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.repository.UserRepository;

@Service
public class PlanningService {

    private final GoalRepository repo;
    private final UserRepository users;
    private final GroqClient ai;
    private final AiUsageService usage;
    private final Map<String, Deque<Long>> calls = new ConcurrentHashMap<>();

    public PlanningService(GoalRepository repo, UserRepository users, GroqClient ai, AiUsageService usage) {
        this.repo = repo;
        this.users = users;
        this.ai = ai;
        this.usage = usage;
    }

    public Responses.GoalView breakdown(Long id, String email) {
        limit(email);
        usage.call(email);
        var user = users.findByEmail(email).orElseThrow();
        var g = repo.findByIdAndUserId(id, user.getId()).orElseThrow();

        double hours = g.getHoursPerDay() == null ? 1.0 : g.getHoursPerDay();
        LocalDate deadline = g.getTargetDate() == null ? LocalDate.now().plusDays(14) : g.getTargetDate();

        var prompt = "Goal: " + g.getTitle()
                + "\nDeadline: " + deadline
                + "\nHours available per day: " + hours
                + "\n" + (g.getDescription() == null ? "" : g.getDescription());

        var r = ai.breakdown(prompt);
        if ("REJECTED".equals(r.status())) {
            usage.rejected(email);
            throw new AiRejectedException(r.reason());
        }

        // Clear existing TODO milestones if regenerating
        if (g.getMilestones().stream().allMatch(m -> m.getStatus() == Milestone.Status.TODO)) {
            g.getMilestones().clear();
        }

        r.milestones().forEach(m -> g.addMilestone(new Milestone(m.title(), m.description(), m.estimatedMinutes(), m.dayNumber())));
        return Responses.GoalView.from(repo.save(g));
    }

    public Responses.GoalView skill(Requests.SkillPlan r, String email) {
        limit(email);
        usage.call(email);
        var user = users.findByEmail(email).orElseThrow();
        LocalDate deadline = LocalDate.now().plusDays(r.days());
        var g = new Goal("Learn " + r.skill(), "A focused " + r.days() + " day plan for " + r.skill() + ".", deadline, r.hoursPerDay(), user);

        var prompt = "Skill: " + r.skill()
                + "\nDuration: " + r.days() + " days"
                + "\nHours available per day: " + r.hoursPerDay()
                + "\nDeadline: " + deadline;

        var aiResult = ai.breakdown(prompt);
        if ("REJECTED".equals(aiResult.status())) {
            usage.rejected(email);
            throw new AiRejectedException(aiResult.reason());
        }

        aiResult.milestones().forEach(m -> g.addMilestone(new Milestone(m.title(), m.description(), m.estimatedMinutes(), m.dayNumber())));
        return Responses.GoalView.from(repo.save(g));
    }

    private void limit(String email) {
        var now = System.currentTimeMillis();
        var q = calls.computeIfAbsent(email, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peek() > 3600000) {
                q.poll();
            }
            if (q.size() >= 10) {
                throw new IllegalStateException("AI rate limit reached. Try again later.");
            }
            q.add(now);
        }
    }
}
