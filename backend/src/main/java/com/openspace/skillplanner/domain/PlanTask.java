package com.openspace.skillplanner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plan_tasks")
public class PlanTask {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_day_id", nullable = false)
    private PlanDay planDay;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "estimated_minutes", nullable = false)
    private int estimatedMinutes;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected PlanTask() {
    }

    public PlanTask(String title, String description, int estimatedMinutes) {
        this.title = title;
        this.description = description;
        this.estimatedMinutes = estimatedMinutes;
    }

    public void setCompleted(boolean completed, Instant completedAt) {
        this.completed = completed;
        this.completedAt = completedAt;
    }

    void setPlanDay(PlanDay planDay) {
        this.planDay = planDay;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
