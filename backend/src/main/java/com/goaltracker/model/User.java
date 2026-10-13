package com.goaltracker.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true) private String email;
    @Column(nullable=false) private String passwordHash;
    @Column(nullable=false) private String displayName;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Role role = Role.USER;
    @Column(nullable=false) private boolean active = true;
    private Long managerId;
    @Column(nullable=false, updatable=false) private Instant createdAt = Instant.now();
    @OneToMany(mappedBy="user", cascade=CascadeType.ALL, orphanRemoval=true) private List<Goal> goals = new ArrayList<>();
    protected User() {}
    public User(String email, String passwordHash, String displayName) { this(email, passwordHash, displayName, Role.USER); }
    public User(String email, String passwordHash, String displayName, Role role) { this.email=email; this.passwordHash=passwordHash; this.displayName=displayName; this.role=role; }
    public Long getId(){return id;} public String getEmail(){return email;} public String getPasswordHash(){return passwordHash;} public String getDisplayName(){return displayName;} public List<Goal> getGoals(){return goals;} public Role getRole(){return role;} public boolean isActive(){return active;} public Long getManagerId(){return managerId;} public Instant getCreatedAt(){return createdAt;}
    public void setRole(Role role){this.role=role;} public void setActive(boolean active){this.active=active;} public void setManagerId(Long managerId){this.managerId=managerId;}
}
