package com.goaltracker.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goaltracker.model.Role;
import com.goaltracker.model.User;
public interface UserRepository extends JpaRepository<User,Long>{ Optional<User> findByEmail(String email); boolean existsByEmail(String email); List<User> findByManagerId(Long managerId); List<User> findByRole(Role role); List<User> findByEmailContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(String email,String displayName); }
