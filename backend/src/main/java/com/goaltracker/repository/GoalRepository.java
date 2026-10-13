package com.goaltracker.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goaltracker.model.Goal;
public interface GoalRepository extends JpaRepository<Goal,Long>{ List<Goal> findByUserIdOrderByTargetDateAsc(Long userId); Optional<Goal> findByIdAndUserId(Long id,Long userId); }
