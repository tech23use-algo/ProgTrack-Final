package com.goaltracker.dto;
import java.time.Instant;
import java.time.LocalDate;
 import java.util.List;

import com.goaltracker.model.Goal;
import com.goaltracker.model.Milestone;
import com.goaltracker.model.Role;
public final class Responses { private Responses(){}
 public record Auth(String token,String email,String displayName,Role role){}
 public record MilestoneView(Long id,String title,String description,Milestone.Status status,Integer estimatedMinutes,Integer dayNumber){}
 public record GoalView(Long id,String title,String description,LocalDate targetDate,Double hoursPerDay,List<MilestoneView> milestones,double completion,Long assignedByManagerId){ public static GoalView from(Goal g){var ms=g.getMilestones().stream().map(m->new MilestoneView(m.getId(),m.getTitle(),m.getDescription(),m.getStatus(),m.getEstimatedMinutes(),m.getDayNumber())).toList(); double p=ms.isEmpty()?0:ms.stream().filter(m->m.status()==Milestone.Status.DONE).count()*100.0/ms.size(); return new GoalView(g.getId(),g.getTitle(),g.getDescription(),g.getTargetDate(),g.getHoursPerDay(),ms,p,g.getAssignedByManagerId());}}
 public record Dashboard(List<GoalView> goals,int totalGoals,double completion,List<GoalView> upcoming){ }
 public record UserSummary(Long id,String email,String displayName,Role role,boolean active,Long managerId,Instant createdAt,double completion,int goalCount){ }
 public record CommentView(Long id,Long managerId,String text,Instant createdAt){ }
 public record TeamAnalytics(double averageCompletion,long overdueGoals,UserSummary mostActive,UserSummary leastActive){ }
 public record AdminStats(long users,long managers,long admins,long goals,double overallCompletion){ }
 public record AiUsage(String day,long calls,long rejected){ }
 public record AuditView(Long id,Long actorId,String action,Long targetId,Instant timestamp){ }
}
