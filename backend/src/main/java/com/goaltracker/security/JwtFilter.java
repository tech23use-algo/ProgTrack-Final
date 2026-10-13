package com.goaltracker.security;
import java.io.IOException;
 import java.util.List;

 import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
 import org.springframework.security.core.authority.SimpleGrantedAuthority;
 import org.springframework.security.core.context.SecurityContextHolder;
 import org.springframework.stereotype.Component;
 import org.springframework.web.filter.OncePerRequestFilter;

import com.goaltracker.repository.UserRepository;

 import jakarta.servlet.FilterChain;
 import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
@Component public class JwtFilter extends OncePerRequestFilter { private final JwtService jwt; private final UserRepository users; public JwtFilter(JwtService jwt,UserRepository users){this.jwt=jwt;this.users=users;} protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{String h=req.getHeader("Authorization");if(h!=null&&h.startsWith("Bearer ")&&jwt.valid(h.substring(7))){String token=h.substring(7);String email=jwt.email(token);var user=users.findByEmail(email).orElse(null);if(user!=null&&user.isActive())SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email,null,List.of(new SimpleGrantedAuthority("ROLE_"+jwt.role(token).name()))));}chain.doFilter(req,res);} }
