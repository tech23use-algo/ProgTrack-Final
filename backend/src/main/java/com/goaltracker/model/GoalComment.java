package com.goaltracker.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class GoalComment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) private Goal goal;
    @Column(nullable=false) private Long managerId;
    @Column(nullable=false, length=2000) private String text;
    @Column(nullable=false, updatable=false) private Instant createdAt = Instant.now();
    protected GoalComment() {}
    public GoalComment(Goal goal, Long managerId, String text){this.goal=goal;this.managerId=managerId;this.text=text;}
    public Long getId(){return id;} public Goal getGoal(){return goal;} public Long getManagerId(){return managerId;} public String getText(){return text;} public Instant getCreatedAt(){return createdAt;}
}
