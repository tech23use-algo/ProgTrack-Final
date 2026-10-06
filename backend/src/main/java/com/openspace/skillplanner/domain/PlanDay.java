package com.openspace.skillplanner.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plan_days")
public class PlanDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private SkillPlan plan;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String goal;

    @OneToMany(mappedBy = "planDay", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderColumn(name = "task_order")
    private List<PlanTask> tasks = new ArrayList<>();

    protected PlanDay() {
    }

    public PlanDay(int dayNumber, String title, String goal) {
        this.dayNumber = dayNumber;
        this.title = title;
        this.goal = goal;
    }

    public void addTask(PlanTask task) {
        tasks.add(task);
        task.setPlanDay(this);
    }

    void setPlan(SkillPlan plan) {
        this.plan = plan;
    }

    public Long getId() {
        return id;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getGoal() {
        return goal;
    }

    public List<PlanTask> getTasks() {
        return tasks;
    }
}
