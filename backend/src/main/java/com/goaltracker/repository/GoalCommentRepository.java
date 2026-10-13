package com.goaltracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goaltracker.model.GoalComment;

public interface GoalCommentRepository extends JpaRepository<GoalComment,Long> { List<GoalComment> findByGoalIdOrderByCreatedAtAsc(Long goalId); }
