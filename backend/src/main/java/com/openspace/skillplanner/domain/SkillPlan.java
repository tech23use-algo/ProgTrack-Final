package com.openspace.skillplanner.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "skill_plans")
public class SkillPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 80)
    private String skill;

    @Column(nullable = false)
    private int days;

    @Column(name = "hours_per_day", nullable = false, precision = 4, scale = 2)
    private BigDecimal hoursPerDay;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderColumn(name = "day_order")
    private List<PlanDay> planDays = new ArrayList<>();

    protected SkillPlan() {
    }

    public SkillPlan(UUID userId, String skill, int days, BigDecimal hoursPerDay, Instant createdAt) {
        this.userId = userId;
        this.skill = skill;
        this.days = days;
        this.hoursPerDay = hoursPerDay;
        this.createdAt = createdAt;
    }

    public void addDay(PlanDay planDay) {
        planDays.add(planDay);
        planDay.setPlan(this);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getSkill() {
        return skill;
    }

    public int getDays() {
        return days;
    }

    public BigDecimal getHoursPerDay() {
        return hoursPerDay;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<PlanDay> getPlanDays() {
        return planDays;
    }
}
