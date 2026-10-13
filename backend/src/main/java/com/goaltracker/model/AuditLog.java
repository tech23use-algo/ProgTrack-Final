package com.goaltracker.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class AuditLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long actorId;
    @Column(nullable=false) private String action;
    private Long targetId;
    @Column(nullable=false, updatable=false) private Instant timestamp = Instant.now();
    protected AuditLog() {}
    public AuditLog(Long actorId,String action,Long targetId){this.actorId=actorId;this.action=action;this.targetId=targetId;}
    public Long getId(){return id;} public Long getActorId(){return actorId;} public String getAction(){return action;} public Long getTargetId(){return targetId;} public Instant getTimestamp(){return timestamp;}
}
