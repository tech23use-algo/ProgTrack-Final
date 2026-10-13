package com.goaltracker.dto;
import java.time.LocalDate;

import com.goaltracker.model.Role;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public final class Requests { private Requests(){}
 public record Register(@NotBlank @Email String email,@NotBlank @Size(min=8,max=100) String password,@NotBlank @Size(max=80) String displayName){}
 public record Login(@NotBlank @Email String email,@NotBlank String password){}
 public record GoalCreate(@NotBlank @Size(min=2,max=120) String title,@Size(max=3000) String description,@FutureOrPresent LocalDate targetDate,@DecimalMin("0.5") @DecimalMax("12") Double hoursPerDay){}
 public record MilestoneCreate(@NotBlank @Size(min=2,max=200) String title,@Size(max=2000) String description,Integer estimatedMinutes,Integer dayNumber){}
 public record MilestoneUpdate(@Size(min=2,max=200) String title,@Size(max=2000) String description,com.goaltracker.model.Milestone.Status status,Boolean toggle){}
 public record SkillPlan(@NotBlank @Size(min=2,max=120) String skill,@NotNull @Min(1) @Max(365) Integer days,@NotNull @DecimalMin("0.5") @DecimalMax("12") Double hoursPerDay){}
 public record ManagerGoal(@NotBlank @Size(min=2,max=120) String title,@Size(max=3000) String description,LocalDate targetDate){}
 public record Comment(@NotBlank @Size(max=2000) String text){}
 public record AdminUser(@NotBlank @Email String email,@NotBlank @Size(min=8,max=100) String password,@NotBlank @Size(max=80) String displayName,Role role){}
 public record RoleChange(@NotNull Role role){}
 public record StatusChange(@NotNull Boolean active){}
 public record ManagerChange(Long managerId){}
}
