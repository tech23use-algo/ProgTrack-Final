package com.goaltracker.service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goaltracker.dto.Requests;
import com.goaltracker.dto.Responses;
import com.goaltracker.model.Goal;
import com.goaltracker.model.Milestone;
import com.goaltracker.model.User;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.repository.MilestoneRepository;
import com.goaltracker.repository.UserRepository;
@Service public class GoalService {private final GoalRepository goals;private final MilestoneRepository milestones;private final UserRepository users;public GoalService(GoalRepository g,MilestoneRepository m,UserRepository u){goals=g;milestones=m;users=u;}private User user(String email){return users.findByEmail(email).orElseThrow();}@Transactional(readOnly=true)public List<Responses.GoalView> list(String email){return goals.findByUserIdOrderByTargetDateAsc(user(email).getId()).stream().map(Responses.GoalView::from).toList();}@Transactional(readOnly=true)public Responses.GoalView get(Long id,String email){return Responses.GoalView.from(owned(id,email));}public Responses.GoalView create(Requests.GoalCreate r,String email){if(r.targetDate()!=null&&r.targetDate().isBefore(LocalDate.now()))throw new IllegalArgumentException("Target date cannot be in the past");double hours=r.hoursPerDay()==null?1.0:r.hoursPerDay();return Responses.GoalView.from(goals.save(new Goal(r.title().trim(),r.description(),r.targetDate(),hours,user(email))));}public void delete(Long id,String email){goals.delete(owned(id,email));}public Responses.MilestoneView add(Long id,Requests.MilestoneCreate r,String email){Goal g=owned(id,email);Milestone m=new Milestone(r.title().trim(),r.description(),r.estimatedMinutes(),r.dayNumber());g.addMilestone(m);goals.save(g);return view(m);}public Responses.MilestoneView update(Long id,Requests.MilestoneUpdate r,String email){Milestone m=milestones.findByIdAndGoalUserId(id,user(email).getId()).orElseThrow(()->new NoSuchElementException("Milestone not found"));m.update(r.title()!=null?r.title().trim():m.getTitle(),r.description()!=null?r.description():m.getDescription(),r.status());if(Boolean.TRUE.equals(r.toggle()))m.toggle();return view(milestones.save(m));}public void deleteMilestone(Long id,String email){milestones.delete(milestones.findByIdAndGoalUserId(id,user(email).getId()).orElseThrow(()->new NoSuchElementException("Milestone not found")));}private Responses.MilestoneView view(Milestone m){return new Responses.MilestoneView(m.getId(),m.getTitle(),m.getDescription(),m.getStatus(),m.getEstimatedMinutes(),m.getDayNumber());}private Goal owned(Long id,String email){return goals.findByIdAndUserId(id,user(email).getId()).orElseThrow(()->new NoSuchElementException("Goal not found"));}}
