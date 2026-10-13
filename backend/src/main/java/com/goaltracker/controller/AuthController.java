package com.goaltracker.controller;
import org.springframework.web.bind.annotation.PostMapping;
 import org.springframework.web.bind.annotation.RequestBody;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RestController;

import com.goaltracker.dto.Requests;
import com.goaltracker.dto.Responses;
import com.goaltracker.service.AuthService;

import jakarta.validation.Valid;
@RestController @RequestMapping("/api/auth") public class AuthController {private final AuthService service;public AuthController(AuthService s){service=s;}@PostMapping("/register")public Responses.Auth register(@Valid @RequestBody Requests.Register r){return service.register(r);}@PostMapping("/login")public Responses.Auth login(@Valid @RequestBody Requests.Login r){return service.login(r);}}
