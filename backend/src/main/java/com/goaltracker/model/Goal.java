package com.goaltracker.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

@Entity
public class Goal {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private String title;
    @Column(length=3000) private String description;
    private LocalDate targetDate;
    private Double hoursPerDay;
    private Long assignedByManagerId;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) private User user;
    @OneToMany(mappedBy="goal", fetch=FetchType.EAGER, cascade=CascadeType.ALL, orphanRemoval=true) @OrderBy("dayNumber ASC, id ASC") private List<Milestone> milestones = new ArrayList<>();
    protected Goal() {}
    public Goal(String title,String description,LocalDate targetDate,User user){this(title,description,targetDate,null,user);}
    public Goal(String title,String description,LocalDate targetDate,Double hoursPerDay,User user){this.title=title;this.description=description;this.targetDate=targetDate;this.hoursPerDay=hoursPerDay;this.user=user;}
    public Long getId(){return id;} public String getTitle(){return title;} public String getDescription(){return description;} public LocalDate getTargetDate(){return targetDate;} public Double getHoursPerDay(){return hoursPerDay;} public Long getAssignedByManagerId(){return assignedByManagerId;} public User getUser(){return user;} public List<Milestone> getMilestones(){return milestones;} public void setAssignedByManagerId(Long id){assignedByManagerId=id;}
    public void addMilestone(Milestone m){milestones.add(m);m.setGoal(this);}
}
