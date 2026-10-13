package com.goaltracker.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goaltracker.dto.Responses;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.repository.UserRepository;
@Service public class DashboardService {private final GoalRepository goals;private final UserRepository users;public DashboardService(GoalRepository g,UserRepository u){goals=g;users=u;}@Transactional(readOnly=true)public Responses.Dashboard get(String email){var all=goals.findByUserIdOrderByTargetDateAsc(users.findByEmail(email).orElseThrow().getId()).stream().map(Responses.GoalView::from).toList();double completion=all.stream().mapToDouble(Responses.GoalView::completion).average().orElse(0);return new Responses.Dashboard(all,all.size(),completion,all.stream().filter(g->g.targetDate()!=null&&!g.targetDate().isBefore(LocalDate.now())).limit(5).toList());}}
