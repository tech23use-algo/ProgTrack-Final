package com.goaltracker.model;

import jakarta.persistence.*;

@Entity
public class Milestone {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private String title;
    @Column(length=2000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status=Status.TODO;
    private Integer estimatedMinutes;
    private Integer dayNumber;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) private Goal goal;
    protected Milestone() {}
    public Milestone(String title,String description,Integer estimatedMinutes,Integer dayNumber){this.title=title;this.description=description;this.estimatedMinutes=estimatedMinutes;this.dayNumber=dayNumber;}
    public Long getId(){return id;} public String getTitle(){return title;} public String getDescription(){return description;} public Status getStatus(){return status;} public Integer getEstimatedMinutes(){return estimatedMinutes;} public Integer getDayNumber(){return dayNumber;} public Goal getGoal(){return goal;} public void setGoal(Goal g){goal=g;}
    public void update(String title,String description,Status status){this.title=title;this.description=description;if(status!=null)this.status=status;} public void toggle(){status=status==Status.DONE?Status.TODO:Status.DONE;}
    public enum Status { TODO, DONE }
}
