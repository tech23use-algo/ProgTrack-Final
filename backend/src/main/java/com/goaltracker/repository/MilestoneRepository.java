package com.goaltracker.repository;
import com.goaltracker.model.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface MilestoneRepository extends JpaRepository<Milestone,Long>{ Optional<Milestone> findByIdAndGoalUserId(Long id,Long userId); }
