package com.goaltracker.service;
import java.util.Locale;

 import org.springframework.security.crypto.password.PasswordEncoder;
 import org.springframework.stereotype.Service;

 import com.goaltracker.dto.Requests;
 import com.goaltracker.dto.Responses;
 import com.goaltracker.model.User;
 import com.goaltracker.repository.UserRepository;
import com.goaltracker.security.JwtService;
@Service public class AuthService { private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt; public AuthService(UserRepository u,PasswordEncoder e,JwtService j){users=u;encoder=e;jwt=j;} public Responses.Auth register(Requests.Register r){String email=r.email().toLowerCase(Locale.ROOT);if(users.existsByEmail(email))throw new IllegalArgumentException("Email is already registered");User u=users.save(new User(email,encoder.encode(r.password()),r.displayName().trim()));return new Responses.Auth(jwt.create(u.getId(),u.getEmail(),u.getRole()),u.getEmail(),u.getDisplayName(),u.getRole());} public Responses.Auth login(Requests.Login r){User u=users.findByEmail(r.email().toLowerCase(Locale.ROOT)).orElseThrow(()->new IllegalArgumentException("Invalid email or password"));if(!u.isActive()||!encoder.matches(r.password(),u.getPasswordHash()))throw new IllegalArgumentException("Invalid email or password");return new Responses.Auth(jwt.create(u.getId(),u.getEmail(),u.getRole()),u.getEmail(),u.getDisplayName(),u.getRole());} }
