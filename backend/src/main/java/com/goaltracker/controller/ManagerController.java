package com.goaltracker.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goaltracker.dto.Requests;
import com.goaltracker.dto.Responses;
import com.goaltracker.repository.GoalCommentRepository;
import com.goaltracker.service.ManagerService;

import jakarta.validation.Valid;
@RestController @RequestMapping("/api/manager") @PreAuthorize("hasRole('MANAGER')") public class ManagerController {private final ManagerService service;private final GoalCommentRepository comments;public ManagerController(ManagerService s,GoalCommentRepository c){service=s;comments=c;}@GetMapping("/team")public List<Responses.UserSummary> team(Authentication a){return service.team(a.getName());}@GetMapping("/team/{userId}/goals")public List<Responses.GoalView> goals(@PathVariable Long userId,Authentication a){return service.memberGoals(a.getName(),userId);}@PostMapping("/team/{userId}/goals")public Responses.GoalView assign(@PathVariable Long userId,@Valid @RequestBody Requests.ManagerGoal r,Authentication a){return service.assignGoal(a.getName(),userId,r);}@PostMapping("/goals/{goalId}/comments")public Responses.CommentView comment(@PathVariable Long goalId,@Valid @RequestBody Requests.Comment r,Authentication a){return service.comment(a.getName(),goalId,r);}@GetMapping("/analytics")public Responses.TeamAnalytics analytics(Authentication a){return service.analytics(a.getName());}}
