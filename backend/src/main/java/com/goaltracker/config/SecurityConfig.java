package com.goaltracker.config;
import java.util.List;

 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
 import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
 import org.springframework.security.config.http.SessionCreationPolicy;
 import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
 import org.springframework.security.crypto.password.PasswordEncoder;
 import org.springframework.security.web.SecurityFilterChain;
 import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
 import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.goaltracker.security.JwtFilter;
@Configuration @EnableMethodSecurity public class SecurityConfig { @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();} @Bean SecurityFilterChain filterChain(HttpSecurity http,JwtFilter filter)throws Exception{return http.csrf(c->c.disable()).cors(c->{}).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(a->a.requestMatchers("/api/auth/**","/error").permitAll().requestMatchers("/api/user/**").hasRole("USER").requestMatchers("/api/manager/**").hasRole("MANAGER").requestMatchers("/api/admin/**").hasRole("ADMIN").anyRequest().authenticated()).addFilterBefore(filter,UsernamePasswordAuthenticationFilter.class).build();} @Bean CorsConfigurationSource cors(@Value("${app.cors.origin}") String origin){var c=new CorsConfiguration();c.setAllowedOriginPatterns(List.of("http://localhost:*","http://127.0.0.1:*"));c.setAllowedMethods(List.of("GET","POST","PATCH","DELETE","OPTIONS"));c.setAllowedHeaders(List.of("*"));c.setAllowCredentials(true);var s=new UrlBasedCorsConfigurationSource();s.registerCorsConfiguration("/**",c);return s;} }
