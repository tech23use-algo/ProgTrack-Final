package com.goaltracker.controller;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goaltracker.dto.Responses;
import com.goaltracker.repository.GoalCommentRepository;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.repository.UserRepository;
@RestController @RequestMapping("/api/user") @PreAuthorize("hasRole('USER')") public class UserCommentController {private final GoalRepository goals;private final UserRepository users;private final GoalCommentRepository comments;public UserCommentController(GoalRepository g,UserRepository u,GoalCommentRepository c){goals=g;users=u;comments=c;}@GetMapping("/goals/{id}/comments")public List<Responses.CommentView> comments(@PathVariable Long id,Authentication a){var g=goals.findByIdAndUserId(id,users.findByEmail(a.getName()).orElseThrow().getId()).orElseThrow(()->new NoSuchElementException("Goal not found"));return comments.findByGoalIdOrderByCreatedAtAsc(g.getId()).stream().map(c->new Responses.CommentView(c.getId(),c.getManagerId(),c.getText(),c.getCreatedAt())).toList();}}
