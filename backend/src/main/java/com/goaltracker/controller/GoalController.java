package com.goaltracker.controller;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goaltracker.dto.Requests;
import com.goaltracker.dto.Responses;
import com.goaltracker.service.DashboardService;
import com.goaltracker.service.GoalService;
import com.goaltracker.service.PlanningService;

import jakarta.validation.Valid;
@RestController @RequestMapping("/api/user") @PreAuthorize("hasRole('USER')") public class GoalController {private final GoalService goals;private final PlanningService planning;private final DashboardService dashboard;public GoalController(GoalService g,PlanningService p,DashboardService d){goals=g;planning=p;dashboard=d;}private String e(Authentication a){return a.getName();}@GetMapping("/goals")public List<Responses.GoalView> list(Authentication a){return goals.list(e(a));}@PostMapping("/goals")public Responses.GoalView create(@Valid @RequestBody Requests.GoalCreate r,Authentication a){return goals.create(r,e(a));}@GetMapping("/goals/{id}")public Responses.GoalView get(@PathVariable Long id,Authentication a){return goals.get(id,e(a));}@DeleteMapping("/goals/{id}")public void delete(@PathVariable Long id,Authentication a){goals.delete(id,e(a));}@PostMapping("/goals/{id}/milestones")public Responses.MilestoneView add(@PathVariable Long id,@Valid @RequestBody Requests.MilestoneCreate r,Authentication a){return goals.add(id,r,e(a));}@PatchMapping("/milestones/{id}")public Responses.MilestoneView update(@PathVariable Long id,@RequestBody Requests.MilestoneUpdate r,Authentication a){return goals.update(id,r,e(a));}@DeleteMapping("/milestones/{id}")public void deleteMilestone(@PathVariable Long id,Authentication a){goals.deleteMilestone(id,e(a));}@PostMapping("/goals/{id}/ai-breakdown")public Responses.GoalView ai(@PathVariable Long id,Authentication a){return planning.breakdown(id,e(a));}@PostMapping("/skill-plans")public Responses.GoalView skill(@Valid @RequestBody Requests.SkillPlan r,Authentication a){return planning.skill(r,e(a));}@GetMapping("/dashboard")public Responses.Dashboard dashboard(Authentication a){return dashboard.get(e(a));}}
