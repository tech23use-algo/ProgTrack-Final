package com.goaltracker.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.goaltracker.dto.Requests;
import com.goaltracker.dto.Responses;
import com.goaltracker.model.Goal;
import com.goaltracker.model.GoalComment;
import com.goaltracker.model.Role;
import com.goaltracker.model.User;
import com.goaltracker.repository.GoalCommentRepository;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.repository.UserRepository;

@Service
public class ManagerService {
    private final UserRepository users; private final GoalRepository goals; private final GoalCommentRepository comments;
    public ManagerService(UserRepository u,GoalRepository g,GoalCommentRepository c){users=u;goals=g;comments=c;}
    private User manager(String email){return users.findByEmail(email).orElseThrow();}
    private User member(String email,Long id){var m=manager(email);var u=users.findById(id).orElseThrow(()->new NoSuchElementException("Team member not found"));if(!Objects.equals(u.getManagerId(),m.getId())||u.getRole()!=Role.USER)throw new NoSuchElementException("Team member not found");return u;}
    public List<Responses.UserSummary> team(String email){return users.findByManagerId(manager(email).getId()).stream().map(this::summary).toList();}
    public List<Responses.GoalView> memberGoals(String email,Long id){member(email,id);return goals.findByUserIdOrderByTargetDateAsc(id).stream().map(Responses.GoalView::from).toList();}
    public Responses.GoalView assignGoal(String email,Long id,Requests.ManagerGoal r){var u=member(email,id);var g=new Goal(r.title(),r.description(),r.targetDate(),u);g.setAssignedByManagerId(manager(email).getId());return Responses.GoalView.from(goals.save(g));}
    public Responses.CommentView comment(String email,Long goalId,Requests.Comment r){var m=manager(email);var g=goals.findById(goalId).orElseThrow(()->new NoSuchElementException("Goal not found"));if(!Objects.equals(g.getUser().getManagerId(),m.getId()))throw new NoSuchElementException("Goal not found");var c=comments.save(new GoalComment(g,m.getId(),r.text().trim()));return new Responses.CommentView(c.getId(),c.getManagerId(),c.getText(),c.getCreatedAt());}
    public Responses.TeamAnalytics analytics(String email){var list=users.findByManagerId(manager(email).getId()).stream().map(this::summary).toList();double avg=list.stream().mapToDouble(Responses.UserSummary::completion).average().orElse(0);long overdue=goals.findAll().stream().filter(g->g.getUser().getManagerId()!=null&&g.getUser().getManagerId().equals(manager(email).getId())&&g.getTargetDate()!=null&&g.getTargetDate().isBefore(LocalDate.now())&&Responses.GoalView.from(g).completion()<100).count();return new Responses.TeamAnalytics(avg,overdue,list.stream().max(Comparator.comparing(Responses.UserSummary::goalCount)).orElse(null),list.stream().min(Comparator.comparing(Responses.UserSummary::goalCount)).orElse(null));}
    private Responses.UserSummary summary(User u){var gs=goals.findByUserIdOrderByTargetDateAsc(u.getId()).stream().map(Responses.GoalView::from).toList();return new Responses.UserSummary(u.getId(),u.getEmail(),u.getDisplayName(),u.getRole(),u.isActive(),u.getManagerId(),u.getCreatedAt(),gs.stream().mapToDouble(Responses.GoalView::completion).average().orElse(0),gs.size());}
}
