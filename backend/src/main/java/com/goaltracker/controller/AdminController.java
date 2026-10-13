package com.goaltracker.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.goaltracker.dto.Requests;
import com.goaltracker.dto.Responses;
import com.goaltracker.model.Role;
import com.goaltracker.service.AdminService;

import jakarta.validation.Valid;
@RestController @RequestMapping("/api/admin") @PreAuthorize("hasRole('ADMIN')") public class AdminController {private final AdminService service;public AdminController(AdminService s){service=s;}@GetMapping("/users")public Object users(@RequestParam(defaultValue="") String search,@RequestParam(required=false) Role role,@RequestParam(defaultValue="0") int page){return service.users(search,role,page);}@PostMapping("/users")public Responses.UserSummary create(@Valid @RequestBody Requests.AdminUser r,Authentication a){return service.create(a.getName(),r);}@PatchMapping("/users/{id}/role")public void role(@PathVariable Long id,@Valid @RequestBody Requests.RoleChange r,Authentication a){service.role(a.getName(),id,r);}@PatchMapping("/users/{id}/status")public void status(@PathVariable Long id,@Valid @RequestBody Requests.StatusChange r,Authentication a){service.status(a.getName(),id,r);}@PatchMapping("/users/{id}/manager")public void manager(@PathVariable Long id,@RequestBody Requests.ManagerChange r,Authentication a){service.manager(a.getName(),id,r);}@DeleteMapping("/users/{id}")public void delete(@PathVariable Long id,Authentication a){service.delete(a.getName(),id);}@GetMapping("/stats")public Responses.AdminStats stats(){return service.stats();}@GetMapping("/ai-usage")public List<Map<String,Object>> usage(){return service.usage();}@GetMapping("/audit-log")public List<Responses.AuditView> audit(){return service.audit();}}
