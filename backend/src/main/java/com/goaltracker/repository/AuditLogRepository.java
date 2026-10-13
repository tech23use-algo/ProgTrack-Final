package com.goaltracker.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.goaltracker.model.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog,Long> { List<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable); }
